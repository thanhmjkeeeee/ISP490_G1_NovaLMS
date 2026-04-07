package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertLessonResponseDTO {
    private Integer lessonId;
    private Integer moduleId;
    private String moduleName;
    private String lessonName;
    private String type;
    private String videoUrl;
    private String videoEmbedUrl; // YouTube embed URL (auto-converted)
    private String contentText;
    private String duration;
    private Boolean allowDownload;
    private Integer orderIndex;

    /** Converts a YouTube watch URL or youtu.be link into an embed-compatible URL. */
    public static String toEmbedUrl(String url) {
        if (url == null || url.isBlank()) return null;
        url = url.trim();

        // Already an embed URL
        if (url.contains("/embed/")) return url;

        // youtu.be/VIDEO_ID
        if (url.contains("youtu.be/")) {
            int idx = url.indexOf("youtu.be/") + 9;
            String rest = url.substring(idx);
            String videoId = rest.contains("?") ? rest.substring(0, rest.indexOf("?")) : rest;
            return "https://www.youtube.com/embed/" + videoId;
        }

        // youtube.com/watch?v=VIDEO_ID
        if (url.contains("youtube.com/watch")) {
            int idx = url.indexOf("v=") + 2;
            String rest = url.substring(idx);
            String videoId = rest.contains("&") ? rest.substring(0, rest.indexOf("&")) : rest;
            return "https://www.youtube.com/embed/" + videoId;
        }

        // youtube.com/shorts/VIDEO_ID
        if (url.contains("/shorts/")) {
            int idx = url.indexOf("/shorts/") + 8;
            String rest = url.substring(idx);
            String videoId = rest.contains("?") ? rest.substring(0, rest.indexOf("?")) : rest;
            return "https://www.youtube.com/embed/" + videoId;
        }

        // Already looks like an embed-ready URL
        if (url.startsWith("https://www.youtube.com/") || url.startsWith("http://www.youtube.com/")) {
            return url.replace("/watch?v=", "/embed/");
        }

        return url;
    }
}
