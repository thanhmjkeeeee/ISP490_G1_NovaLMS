package com.example.DoAn.service.impl;

import com.example.DoAn.service.ITextToSpeechService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Primary
@Slf4j
public class FreeTTSServiceImpl implements ITextToSpeechService {

    private static final String TTS_URL = "https://translate.google.com/translate_tts?ie=UTF-8&tl=en-US&client=tw-ob&q=";
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    @Override
    public byte[] synthesize(String text) {
        return synthesizeWithVoice(text, "en_us_001"); // Default female
    }

    @Override
    public byte[] synthesizeDialogue(String transcript) {
        if (transcript == null || transcript.isBlank()) return null;
        
        // Expanded voice pools for maximum variety
        String[] maleVoices = {
            "en_us_006", "en_us_010", "en_au_002", "en_uk_001", "en_male_narration", 
            "en_us_rocket", "en_male_funny", "en_male_c3_sunshine"
        };
        String[] femaleVoices = {
            "en_us_001", "en_us_002", "en_au_001", "en_uk_003", "en_female_emotional", 
            "en_us_009", "en_female_samcent", "en_female_makeup"
        };
        
        java.util.Map<String, String> characterVoiceMap = new java.util.HashMap<>();
        int maleIdx = 0, femaleIdx = 0;

        try {
            String[] lines = transcript.split("\\r?\\n");
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.*?)\\[(Male|Female)\\]\\s*:\\s*(.*)$", java.util.regex.Pattern.CASE_INSENSITIVE);

            java.util.List<java.util.concurrent.CompletableFuture<byte[]>> futures = new java.util.ArrayList<>();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                java.util.regex.Matcher matcher = pattern.matcher(line);
                String voice = null;
                String textToSpeak = line;
                String charName = "Default";
                boolean isMale = false;

                if (matcher.find()) {
                    charName = matcher.group(1).trim();
                    if (charName.isEmpty()) charName = "Unknown";
                    String gender = matcher.group(2).toLowerCase();
                    textToSpeak = matcher.group(3);
                    isMale = gender.contains("male") || gender.contains("nam");
                } else if (line.contains(":")) {
                    int colonIdx = line.indexOf(":");
                    charName = line.substring(0, colonIdx).trim();
                    textToSpeak = line.substring(colonIdx + 1).trim();
                    String lowName = charName.toLowerCase();
                    String[] maleKeywords = {"male", "man", "mr", "boy", "john", "robert", "david", "paul", "mark", "kevin", "james", "michael", "william", "richard", "thomas", "charles", "christopher", "dan", "bob", "sam"};
                    isMale = java.util.Arrays.stream(maleKeywords).anyMatch(lowName::contains);
                }

                if (!characterVoiceMap.containsKey(charName)) {
                    if (isMale) {
                        voice = maleVoices[maleIdx % maleVoices.length];
                        maleIdx++;
                    } else {
                        voice = femaleVoices[femaleIdx % femaleVoices.length];
                        femaleIdx++;
                    }
                    characterVoiceMap.put(charName, voice);
                } else {
                    voice = characterVoiceMap.get(charName);
                }

                final String finalVoice = voice;
                final String finalText = textToSpeak;
                final String finalChar = charName;
                
                futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    log.info("[TTS-PARA] Starting: '{}'", finalChar);
                    return synthesizeWithVoice(finalText, finalVoice);
                }));
            }

            // Wait for all to complete and combine in order
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                for (java.util.concurrent.CompletableFuture<byte[]> future : futures) {
                    byte[] audio = future.get();
                    if (audio != null) {
                        outputStream.write(audio);
                    }
                }
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("Error synthesizing dialogue in parallel: {}", e.getMessage());
            return null;
        }
    }

    private byte[] synthesizeWithVoice(String text, String voice) {
        if (text == null || text.isBlank()) return null;
        // Standard Google Translate TTS for reliability as requested
        return callTranslateTTS(text, voice);
    }



    private byte[] callTranslateTTS(String text, String voice) {
        try {
            // Map TikTok voice pool to Google Translate regional codes for variety
            String tl = "en-US";
            if (voice.contains("uk")) tl = "en-GB";
            else if (voice.contains("au")) tl = "en-AU";
            else if (voice.equals("en_us_002")) tl = "en-IN";
            else if (voice.equals("en_us_010")) tl = "en-IE";

            String[] chunks = splitText(text, 180);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                for (String chunk : chunks) {
                    String encodedText = URLEncoder.encode(chunk, StandardCharsets.UTF_8);
                    String url = "https://translate.google.com/translate_tts?ie=UTF-8&tl=" + tl + "&client=tw-ob&q=" + encodedText;
                    
                    Request request = new Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0")
                            .build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            outputStream.write(response.body().bytes());
                        }
                    }
                }
                byte[] result = outputStream.toByteArray();
                return result.length > 0 ? result : null;
            }
        } catch (Exception e) {
            log.error("Google TTS fallback failed: {}", e.getMessage());
            return null;
        }
    }

    private String[] splitText(String text, int limit) {
        // Simple splitting by spaces
        java.util.List<String> parts = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() + word.length() + 1 > limit) {
                parts.add(sb.toString());
                sb = new StringBuilder();
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append(word);
        }
        if (sb.length() > 0) parts.add(sb.toString());
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
