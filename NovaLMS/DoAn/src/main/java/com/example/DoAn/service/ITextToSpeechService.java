package com.example.DoAn.service;

public interface ITextToSpeechService {
    /**
     * Chuyển văn bản thành audio (MP3).
     * @param text Văn bản cần chuyển.
     * @return Mảng byte chứa dữ liệu audio MP3.
     */
    byte[] synthesize(String text);

    /**
     * Chuyển văn bản có cấu trúc hội thoại (Speaker labels) thành audio đa giọng nói.
     * @param transcript Văn bản có nhãn [Male]/[Female].
     * @return Mảng byte audio MP3.
     */
    byte[] synthesizeDialogue(String transcript);
}
