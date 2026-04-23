package com.example.DoAn.service.impl;

import com.example.DoAn.service.ITextToSpeechService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TTS Service with two-tier strategy:
 *
 *  1. ElevenLabs (primary) — real male/female neural voices, 10K chars/month free
 *     Configure: elevenlabs.api.key in application.properties
 *     Sign up:   https://elevenlabs.io
 *
 *  2. Google Translate TTS (fallback) — no key, different locales per character
 *     Accent varies by locale: en-GB, en-US, en-AU, en-IN, en-IE
 *     (All female-sounding but accent is genuinely different per character)
 */
@Service
@Primary
@Slf4j
public class FreeTTSServiceImpl implements ITextToSpeechService {

    // ── ElevenLabs config ────────────────────────────────────────────────────
    private static final String EL_BASE = "https://api.elevenlabs.io/v1/text-to-speech/%s";

    // Built-in voices (no custom voice cloning required, always available on free tier)
    // Male voices
    private static final String EL_MALE_1   = "TxGEqnHWrfWFTfGW9XjX"; // Josh – deep, calm
    private static final String EL_MALE_2   = "VR6AewLTigWG4xSOukaG"; // Arnold – crisp
    private static final String EL_MALE_3   = "pNInz6obpgDQGcFmaJgB"; // Adam – authoritative
    private static final String EL_MALE_4   = "yoZ06aMxZJJ28mfd3POQ"; // Sam – young
    // Female voices
    private static final String EL_FEMALE_1 = "21m00Tcm4TlvDq8ikWAM"; // Rachel – calm, professional
    private static final String EL_FEMALE_2 = "EXAVITQu4vr4xnSDxMaL"; // Bella – soft
    private static final String EL_FEMALE_3 = "MF3mGyEYCl7XYWbV9V6O"; // Elli – bright
    private static final String EL_FEMALE_4 = "AZnzlk1XvdvUeBnXmlld"; // Domi – strong

    private static final String[] EL_MALE_POOL   = { EL_MALE_1, EL_MALE_2, EL_MALE_3, EL_MALE_4 };
    private static final String[] EL_FEMALE_POOL = { EL_FEMALE_1, EL_FEMALE_2, EL_FEMALE_3, EL_FEMALE_4 };

    // ── Google Translate TTS fallback ─────────────────────────────────────────
    private static final String GTTS_BASE =
            "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=%s&q=%s";

    private static final String[] GTTS_MALE_LOCALES   = { "en-GB", "en-IE", "en-AU", "en-IN" };
    private static final String[] GTTS_FEMALE_LOCALES = { "en-US", "en-AU", "en-IN", "en-GB" };

    // ── Injected config ───────────────────────────────────────────────────────
    @Value("${elevenlabs.api.key:}")
    private String elApiKey;

    // ── HTTP client ───────────────────────────────────────────────────────────
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON_MT = MediaType.parse("application/json; charset=utf-8");

    // ── Male name heuristic ───────────────────────────────────────────────────
    private static final Set<String> MALE_NAMES = new HashSet<>(Arrays.asList(
            "male", "man", "mr", "boy", "sir",
            "john", "robert", "david", "paul", "mark", "kevin", "james",
            "michael", "william", "richard", "thomas", "charles", "daniel",
            "christopher", "dan", "bob", "sam", "alex", "ben", "chris",
            "peter", "andrew", "jack", "tom", "mike", "george", "henry",
            "brian", "matthew", "ryan", "luke", "adam", "simon",
            "officer", "host", "examiner", "interviewer", "narrator male"
    ));

    // ────────────────────────────────────────────────────────────────────────

    @Override
    public byte[] synthesize(String text) {
        if (hasElKey()) {
            byte[] result = callElevenLabs(text, EL_FEMALE_1);
            if (result != null) return result;
        }
        return callGoogleTTS(text, "en-GB");
    }

    @Override
    public byte[] synthesizeDialogue(String transcript) {
        if (transcript == null || transcript.isBlank()) return null;

        boolean useEl = hasElKey();
        Map<String, Object> characterVoiceMap = new HashMap<>(); // value: String (EL id or locale)
        int maleIdx = 0, femaleIdx = 0;

        Pattern pattern = Pattern.compile(
                "^(.*?)(?:\\[(Male|Female)])?\\s*:\\s*(.*)$",
                Pattern.CASE_INSENSITIVE);

        try {
            String[] lines = transcript.split("\\r?\\n");
            List<CompletableFuture<byte[]>> futures = new ArrayList<>();

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty()) continue;

                Matcher matcher = pattern.matcher(line);
                String charName    = "Narrator";
                String textToSpeak = line;
                boolean isMale     = false;

                if (matcher.matches()) {
                    charName     = matcher.group(1).trim();
                    String gTag  = matcher.group(2);
                    textToSpeak  = matcher.group(3).trim();
                    if (charName.isEmpty()) charName = "Narrator";
                    isMale = gTag != null ? gTag.equalsIgnoreCase("Male") : isMaleName(charName);
                } else if (line.contains(":")) {
                    int idx  = line.indexOf(':');
                    charName     = line.substring(0, idx).trim();
                    textToSpeak  = line.substring(idx + 1).trim();
                    isMale       = isMaleName(charName);
                }

                if (!characterVoiceMap.containsKey(charName)) {
                    String voiceId;
                    if (useEl) {
                        voiceId = isMale
                                ? EL_MALE_POOL[maleIdx++ % EL_MALE_POOL.length]
                                : EL_FEMALE_POOL[femaleIdx++ % EL_FEMALE_POOL.length];
                        log.info("[TTS-EL] '{}' ({}) → voiceId={}", charName, isMale ? "Male" : "Female", voiceId);
                    } else {
                        voiceId = isMale
                                ? GTTS_MALE_LOCALES[maleIdx++ % GTTS_MALE_LOCALES.length]
                                : GTTS_FEMALE_LOCALES[femaleIdx++ % GTTS_FEMALE_LOCALES.length];
                        log.info("[TTS-Google] '{}' ({}) → locale={}", charName, isMale ? "Male" : "Female", voiceId);
                    }
                    characterVoiceMap.put(charName, voiceId);
                }

                final String voice     = (String) characterVoiceMap.get(charName);
                final String finalText = textToSpeak;
                final boolean el       = useEl;

                futures.add(CompletableFuture.supplyAsync(() -> {
                    if (el) {
                        byte[] audio = callElevenLabs(finalText, voice);
                        if (audio != null) return audio;
                        // fallback to Google if EL fails
                        log.warn("[TTS-EL] Failed, falling back to Google TTS");
                        return callGoogleTTS(finalText, "en-GB");
                    }
                    return callGoogleTTS(finalText, voice);
                }));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                for (CompletableFuture<byte[]> f : futures) {
                    byte[] audio = f.get();
                    if (audio != null && audio.length > 0) out.write(audio);
                }
                byte[] result = out.toByteArray();
                log.info("[TTS] Dialogue done — {} bytes (engine={})", result.length, useEl ? "ElevenLabs" : "Google");
                return result.length > 0 ? result : null;
            }

        } catch (Exception e) {
            log.error("[TTS] Dialogue synthesis failed: {}", e.getMessage());
            return null;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // ElevenLabs
    // ────────────────────────────────────────────────────────────────────────

    private byte[] callElevenLabs(String text, String voiceId) {
        if (text == null || text.isBlank() || !hasElKey()) return null;
        try {
            // ElevenLabs max ~2500 chars/request — chunk if needed
            String[] chunks = splitText(text, 2000);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                for (String chunk : chunks) {
                    String url = String.format(EL_BASE, voiceId);
                    String body = String.format(
                            "{\"text\":\"%s\",\"model_id\":\"eleven_flash_v2_5\"," +
                            "\"voice_settings\":{\"stability\":0.5,\"similarity_boost\":0.75}}",
                            chunk.replace("\\", "\\\\").replace("\"", "\\\"")
                                 .replace("\n", " ").replace("\r", ""));

                    Request req = new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(body, JSON_MT))
                            .header("xi-api-key", elApiKey)
                            .header("Accept", "audio/mpeg")
                            .build();

                    try (Response resp = httpClient.newCall(req).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            out.write(resp.body().bytes());
                        } else {
                            log.warn("[TTS-EL] HTTP {}: {}", resp.code(), resp.message());
                            return null;
                        }
                    }
                }
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.warn("[TTS-EL] Exception: {}", e.getMessage());
            return null;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Google Translate TTS (fallback)
    // ────────────────────────────────────────────────────────────────────────

    private byte[] callGoogleTTS(String text, String locale) {
        if (text == null || text.isBlank()) return null;
        try {
            String[] chunks = splitText(text, 180);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                for (String chunk : chunks) {
                    String encoded = URLEncoder.encode(chunk, StandardCharsets.UTF_8);
                    String url = String.format(GTTS_BASE, locale, encoded);
                    Request req = new Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            .build();
                    try (Response resp = httpClient.newCall(req).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            out.write(resp.body().bytes());
                        }
                    }
                }
                byte[] result = out.toByteArray();
                return result.length > 0 ? result : null;
            }
        } catch (Exception e) {
            log.error("[TTS-Google] Exception locale={}: {}", locale, e.getMessage());
            return null;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private boolean hasElKey() {
        return elApiKey != null && !elApiKey.isBlank();
    }

    private boolean isMaleName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return MALE_NAMES.stream().anyMatch(lower::contains);
    }

    private String[] splitText(String text, int limit) {
        List<String> parts = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() + word.length() + 1 > limit) {
                if (sb.length() > 0) { parts.add(sb.toString().trim()); sb = new StringBuilder(); }
            }
            if (sb.length() > 0) sb.append(' ');
            sb.append(word);
        }
        if (sb.length() > 0) parts.add(sb.toString().trim());
        return parts.toArray(new String[0]);
    }




    private byte[] tryTiklyDown(String text, String voice) throws Exception {
        String url = "https://api.tiklydown.eu.org/api/main/tts?query=" +
                URLEncoder.encode(text, StandardCharsets.UTF_8) + "&voice=" + voice;
        Request request = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body().string());
            if (node.has("result") && node.get("result").has("data")) {
                return java.util.Base64.getDecoder().decode(node.get("result").get("data").asText());
            }
        }
        return null;
    }


    private byte[] tryVercel(String text, String voice) throws Exception {
        String url = "https://tiktok-tts-api.vercel.app/api/tts?text=" +
                URLEncoder.encode(text, StandardCharsets.UTF_8) + "&voice=" + voice;
        Request request = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body().string());
            if (node.has("data")) return java.util.Base64.getDecoder().decode(node.get("data").asText());
        }
        return null;
    }

    private byte[] tryCountik(String text, String voice) throws Exception {
        String url = "https://countik.com/api/text/speech";
        String jsonBody = String.format("{\"text\": \"%s\", \"voice\": \"%s\"}", text.replace("\"", "\\\""), voice);
        Request request = new Request.Builder().url(url).post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                .header("User-Agent", "Mozilla/5.0").header("Origin", "https://countik.com").header("Referer", "https://countik.com/").build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body().string());
            if (node.has("v_data")) return java.util.Base64.getDecoder().decode(node.get("v_data").asText());
        }
        return null;
    }
}
