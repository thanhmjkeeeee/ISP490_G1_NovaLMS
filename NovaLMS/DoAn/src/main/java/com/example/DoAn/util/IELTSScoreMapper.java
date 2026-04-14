package com.example.DoAn.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;

public class IELTSScoreMapper {

    private static final TreeMap<Integer, Double> READING_ACADEMIC_MAP = new TreeMap<>();
    private static final TreeMap<Integer, Double> LISTENING_MAP = new TreeMap<>();

    static {
        // Listening mapping (out of 40)
        LISTENING_MAP.put(0, 0.0);
        LISTENING_MAP.put(2, 1.0);
        LISTENING_MAP.put(4, 2.5);
        LISTENING_MAP.put(6, 3.0);
        LISTENING_MAP.put(8, 3.5);
        LISTENING_MAP.put(10, 4.0);
        LISTENING_MAP.put(13, 4.5);
        LISTENING_MAP.put(16, 5.0);
        LISTENING_MAP.put(19, 5.5);
        LISTENING_MAP.put(23, 6.0);
        LISTENING_MAP.put(27, 6.5);
        LISTENING_MAP.put(30, 7.0);
        LISTENING_MAP.put(33, 7.5);
        LISTENING_MAP.put(35, 8.0);
        LISTENING_MAP.put(37, 8.5);
        LISTENING_MAP.put(39, 9.0);

        // Reading Academic mapping (out of 40)
        READING_ACADEMIC_MAP.put(0, 0.0);
        READING_ACADEMIC_MAP.put(2, 1.0);
        READING_ACADEMIC_MAP.put(4, 2.5);
        READING_ACADEMIC_MAP.put(6, 3.0);
        READING_ACADEMIC_MAP.put(8, 3.5);
        READING_ACADEMIC_MAP.put(10, 4.0);
        READING_ACADEMIC_MAP.put(13, 4.5);
        READING_ACADEMIC_MAP.put(15, 5.0);
        READING_ACADEMIC_MAP.put(19, 5.5);
        READING_ACADEMIC_MAP.put(23, 6.0);
        READING_ACADEMIC_MAP.put(27, 6.5);
        READING_ACADEMIC_MAP.put(30, 7.0);
        READING_ACADEMIC_MAP.put(33, 7.5);
        READING_ACADEMIC_MAP.put(35, 8.0);
        READING_ACADEMIC_MAP.put(37, 8.5);
        READING_ACADEMIC_MAP.put(39, 9.0);
    }

    /**
     * Map raw points to IELTS Band 0-9.
     * If fewer than 40 questions, pro-rate to 40 first.
     */
    public static double mapRawToBand(double rawScore, double maxScore, String skill) {
        if (maxScore <= 0) return 0.0;
        
        // Pro-rate to 40 questions
        double equivalent40 = (rawScore / maxScore) * 40.0;
        int roundedScore = (int) Math.round(equivalent40);

        TreeMap<Integer, Double> map = "LISTENING".equalsIgnoreCase(skill) ? LISTENING_MAP : READING_ACADEMIC_MAP;
        
        Map.Entry<Integer, Double> entry = map.floorEntry(roundedScore);
        return entry != null ? entry.getValue() : 0.0;
    }

    /**
     * Round average score to the nearest 0.5 (IELTS rule).
     * .125 -> .0
     * .25 -> .5
     * .75 -> 1.0
     */
    public static double roundToIELTS(double average) {
        double floor = Math.floor(average);
        double fraction = average - floor;
        if (fraction < 0.25) return floor;
        if (fraction < 0.75) return floor + 0.5;
        return floor + 1.0;
    }

    public static double calculateOverallBand(Map<String, Double> skillBands) {
        if (skillBands.isEmpty()) return 0.0;
        double sum = 0;
        int count = 0;
        for (Double band : skillBands.values()) {
            if (band != null) {
                sum += band;
                count++;
            }
        }
        if (count == 0) return 0.0;
        return roundToIELTS(sum / count);
    }
}
