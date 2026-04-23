package com.example.DoAn.service.impl;

import com.example.DoAn.service.ITextToSpeechService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleTTSServiceImpl implements ITextToSpeechService {

    @Value("${google.api.key:${ai.api.key:}}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private static final String TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";
    private static final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public byte[] synthesize(String text) {
        return callGoogleTTS(text, false);
    }

    @Override
    public byte[] synthesizeDialogue(String transcript) {
        if (transcript == null || transcript.isBlank()) return null;
        String ssml = transcriptToSsml(transcript);
        return callGoogleTTS(ssml, true);
    }

    private byte[] callGoogleTTS(String input, boolean isSsml) {
        try {
            Map<String, Object> body = new HashMap<>();
            Map<String, String> inputMap = new HashMap<>();
            inputMap.put(isSsml ? "ssml" : "text", input);
            body.put("input", inputMap);

            Map<String, String> voice = new HashMap<>();
            voice.put("languageCode", "en-US");
            // Default voice if not using multi-voice SSML
            if (!isSsml) {
                voice.put("name", "en-US-Wavenet-B"); 
            }
            body.put("voice", voice);

            Map<String, String> audioConfig = new HashMap<>();
            audioConfig.put("audioEncoding", "MP3");
            body.put("audioConfig", audioConfig);

            String jsonBody = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                    .url(TTS_URL + "?key=" + apiKey)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Google TTS API Error: {} - {}", response.code(), err);
                    return null;
                }

                Map<String, String> respMap = objectMapper.readValue(response.body().string(), Map.class);
                String audioContent = respMap.get("audioContent");
                if (audioContent == null) return null;

                return Base64.getDecoder().decode(audioContent);
            }
        } catch (Exception e) {
            log.error("Error calling Google TTS: {}", e.getMessage());
            return null;
        }
    }

    private String transcriptToSsml(String transcript) {
        StringBuilder sb = new StringBuilder("<speak>");
        String[] lines = transcript.split("\n");
        
        // Pattern to match "Name [Gender]: Text" or just "[Gender]: Text"
        Pattern pattern = Pattern.compile("^(.*?)\\[(Male|Female)\\]\\s*:\\s*(.*)$", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String gender = matcher.group(2).toLowerCase();
                String text = matcher.group(3);
                String voiceName = gender.contains("male") ? "en-US-Wavenet-B" : "en-US-Wavenet-F";
                
                sb.append("<voice name=\"").append(voiceName).append("\">")
                  .append(escapeXml(text))
                  .append("</voice><break time=\"600ms\"/>");
            } else {
                // Fallback for lines without tags
                sb.append(escapeXml(line)).append("<break time=\"500ms\"/>");
            }
        }
        sb.append("</speak>");
        return sb.toString();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
