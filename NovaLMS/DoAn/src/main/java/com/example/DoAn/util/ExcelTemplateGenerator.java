package com.example.DoAn.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ExcelTemplateGenerator {

    public byte[] generate(String questionType) throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Câu hỏi");

        CellStyle headerStyle = buildHeaderStyle(wb);
        CellStyle noteStyle = buildNoteStyle(wb);

        switch (questionType) {
            case "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI" -> createMCTemplate(sheet, headerStyle, noteStyle, questionType);
            case "FILL_IN_BLANK" -> createFillBlankTemplate(sheet, headerStyle, noteStyle);
            case "MATCHING" -> createMatchingTemplate(sheet, headerStyle, noteStyle);
            case "WRITING" -> createWritingTemplate(sheet, headerStyle, noteStyle);
            case "SPEAKING" -> createSpeakingTemplate(sheet, headerStyle, noteStyle);
            case "QUESTION_GROUP" -> createGroupTemplate(wb, headerStyle, noteStyle);
            default -> throw new IllegalArgumentException("Loại câu hỏi không hợp lệ: " + questionType);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.close();
        return baos.toByteArray();
    }

    private void createMCTemplate(Sheet sheet, CellStyle header, CellStyle note, String type) {
        String[] headers = {"Content *", "Option A", "Option B", "Option C", "Option D",
                type.equals("MULTIPLE_CHOICE_MULTI") ? "CorrectAnswers (VD: A,B)" : "CorrectAnswer (VD: A)",
                "Skill (LISTENING/READING/WRITING/SPEAKING)", "CEFR (A1-C2)", "Topic", "Explanation"};
        int[] widths = {8000, 3000, 3000, 3000, 3000, 4000, 4000, 2000, 3000, 6000};

        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(header);
            sheet.setColumnWidth(i, widths[i]);
        }

        Row nRow = sheet.createRow(1);
        Cell noteCell = nRow.createCell(0);
        noteCell.setCellValue(type.equals("MULTIPLE_CHOICE_MULTI")
                ? "Chú ý: CorrectAnswers phân tách bằng dấu phẩy, VD: A,B,C"
                : "Chú ý: CorrectAnswer là chữ cái của đáp án đúng, VD: B");
        noteCell.setCellStyle(note);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        // Sample data row
        Row sample = sheet.createRow(2);
        sample.createCell(0).setCellValue("What is the capital of France?");
        sample.createCell(1).setCellValue("London");
        sample.createCell(2).setCellValue("Paris");
        sample.createCell(3).setCellValue("Berlin");
        sample.createCell(4).setCellValue("Madrid");
        sample.createCell(5).setCellValue("B");
        sample.createCell(6).setCellValue("READING");
        sample.createCell(7).setCellValue("A1");
        sample.createCell(8).setCellValue("Geography");
        sample.createCell(9).setCellValue("Paris is the capital city of France.");

        sheet.createRow(3); // blank for user
    }

    private void createFillBlankTemplate(Sheet sheet, CellStyle header, CellStyle note) {
        String[] headers = {"Content * (dùng ___ cho chỗ trống)", "CorrectAnswer *",
                "Skill", "CEFR", "Topic", "Explanation"};
        int[] widths = {10000, 4000, 3000, 2000, 3000, 6000};

        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(header);
            sheet.setColumnWidth(i, widths[i]);
        }

        Row nRow = sheet.createRow(1);
        Cell nc = nRow.createCell(0);
        nc.setCellValue("Chú ý: Dùng ___ (3 dấu gạch dưới) cho chỗ trống cần điền.");
        nc.setCellStyle(note);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

        Row sample = sheet.createRow(2);
        sample.createCell(0).setCellValue("The capital of France is ___.");
        sample.createCell(1).setCellValue("Paris");
        sample.createCell(2).setCellValue("READING");
        sample.createCell(3).setCellValue("A1");
        sample.createCell(4).setCellValue("Geography");
        sample.createCell(5).setCellValue("France's capital is Paris.");
        sheet.createRow(3);
    }

    private void createMatchingTemplate(Sheet sheet, CellStyle header, CellStyle note) {
        String[] headers = {"Content *",
                "MatchLeft (VD: Apple|Banana|Car)",
                "MatchRight (VD: Quả táo|Quả chuối|Ô tô)",
                "CorrectPairs (VD: 1,2,3)",
                "Skill", "CEFR", "Topic"};
        int[] widths = {8000, 6000, 6000, 4000, 3000, 2000, 3000};

        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(header);
            sheet.setColumnWidth(i, widths[i]);
        }

        Row nRow = sheet.createRow(1);
        Cell nc = nRow.createCell(0);
        nc.setCellValue("Chú ý: MatchLeft và MatchRight phân tách bằng dấu |, số phần tử phải bằng nhau. CorrectPairs: thứ tự ghép nối, VD 1,2,3 nghĩa là item trái 1 ghép với item phải 1.");
        nc.setCellStyle(note);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

        Row sample = sheet.createRow(2);
        sample.createCell(0).setCellValue("Match each word with its meaning.");
        sample.createCell(1).setCellValue("Apple|Banana|Car");
        sample.createCell(2).setCellValue("Quả táo|Quả chuối|Ô tô");
        sample.createCell(3).setCellValue("1,2,3");
        sample.createCell(4).setCellValue("READING");
        sample.createCell(5).setCellValue("A1");
        sample.createCell(6).setCellValue("Food");
        sheet.createRow(3);
    }

    private void createWritingTemplate(Sheet sheet, CellStyle header, CellStyle note) {
        String[] headers = {"Content *", "Skill", "CEFR", "Topic", "Explanation"};
        int[] widths = {12000, 3000, 2000, 3000, 6000};

        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(header);
            sheet.setColumnWidth(i, widths[i]);
        }

        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("Write a paragraph about your favorite holiday destination (80-100 words).");
        sample.createCell(1).setCellValue("WRITING");
        sample.createCell(2).setCellValue("B1");
        sample.createCell(3).setCellValue("Travel");
        sample.createCell(4).setCellValue("Focus on description and reasons.");
        sheet.createRow(2);
    }

    private void createSpeakingTemplate(Sheet sheet, CellStyle header, CellStyle note) {
        String[] headers = {"Content *", "AudioUrl (public URL)", "Skill", "CEFR", "Topic"};
        int[] widths = {12000, 6000, 3000, 2000, 3000};

        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(header);
            sheet.setColumnWidth(i, widths[i]);
        }

        Row nRow = sheet.createRow(1);
        Cell nc = nRow.createCell(0);
        nc.setCellValue("Chú ý: Upload audio lên Cloudinary trước, dán public URL vào AudioUrl. Để trống nếu chưa có.");
        nc.setCellStyle(note);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

        Row sample = sheet.createRow(2);
        sample.createCell(0).setCellValue("Describe your favorite food and explain why you like it (1-2 minutes).");
        sample.createCell(1).setCellValue("https://res.cloudinary.com/.../sample.mp3");
        sample.createCell(2).setCellValue("SPEAKING");
        sample.createCell(3).setCellValue("B1");
        sample.createCell(4).setCellValue("Food");
        sheet.createRow(3);
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        Font bold = wb.createFont();
        bold.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(bold);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle buildNoteStyle(Workbook wb) {
        Font italic = wb.createFont();
        italic.setItalic(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(italic);
        style.setWrapText(true);
        return style;
    }

    private void createGroupTemplate(Workbook wb, CellStyle header, CellStyle note) {
        // Sheet 1: Group Info
        Sheet groupSheet = wb.createSheet("Group Info");
        String[] groupHeaders = {"PASSAGE *", "SKILL", "CEFR", "TOPIC", "AUDIO_URL", "IMAGE_URL", "EXPLANATION"};
        int[] groupWidths = {15000, 3000, 2000, 3000, 5000, 5000, 6000};

        Row gh0 = groupSheet.createRow(0);
        for (int i = 0; i < groupHeaders.length; i++) {
            Cell cell = gh0.createCell(i);
            cell.setCellValue(groupHeaders[i]);
            cell.setCellStyle(header);
            groupSheet.setColumnWidth(i, groupWidths[i]);
        }

        // Row 1: NOTE — passage metadata must be entered in Row 2 (index 1 = Excel row 2)
        Row gh1 = groupSheet.createRow(1);
        gh1.createCell(0).setCellValue("--- HƯỚNG DẪN: Điền PASSAGE ở Row 2 (dòng này) | Điền câu hỏi con ở Sheet 2 ---");
        gh1.getCell(0).setCellStyle(note);
        gh1.createCell(1).setCellValue("--- Câu hỏi con: xem Sheet 2 bắt đầu từ Row 2 ---");
        gh1.getCell(1).setCellStyle(note);
        groupSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));
        gh1.createCell(2).setCellValue("Row 1=Header, Row 2=NOTE, Row 3=PASSAGE data, Row 4+=optional");
        gh1.getCell(2).setCellStyle(note);
        groupSheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 6));

        // Row 2: blank (user fills in their passage data)
        groupSheet.createRow(2);

        // Sheet 2: Child Questions
        Sheet childSheet = wb.createSheet("Child Questions");
        String[] childHeaders = {"CONTENT *", "TYPE", "OPTA", "OPTB", "OPTC", "OPTD",
                "CORRECT/ANSWER", "MATCH_LEFT(|sep)", "MATCH_RIGHT(|sep)", "PAIRS(,sep)",
                "SUB_SKILL", "SUB_CEFR", "SUB_TOPIC", "SUB_EXPLANATION"};
        int[] childWidths = {8000, 3000, 3000, 3000, 3000, 3000, 3000, 4000, 4000, 3000, 3000, 2000, 3000, 6000};

        Row ch0 = childSheet.createRow(0);
        for (int i = 0; i < childHeaders.length; i++) {
            Cell cell = ch0.createCell(i);
            cell.setCellValue(childHeaders[i]);
            cell.setCellStyle(header);
            childSheet.setColumnWidth(i, childWidths[i]);
        }

        // Row 1: blank (user fills in their first child question, data starts at index 1 = Excel row 2)
        // Sample data from index 2 onwards
        Row ch2 = childSheet.createRow(2);
        ch2.createCell(0).setCellValue("What is the main idea of the passage?");
        ch2.createCell(1).setCellValue("MULTIPLE_CHOICE_SINGLE");
        ch2.createCell(2).setCellValue("Option A text");
        ch2.createCell(3).setCellValue("Option B text");
        ch2.createCell(4).setCellValue("Option C text");
        ch2.createCell(5).setCellValue("Option D text");
        ch2.createCell(6).setCellValue("A");

        Row ch3 = childSheet.createRow(3);
        ch3.createCell(0).setCellValue("Complete the sentence: The author believes that ____.");
        ch3.createCell(1).setCellValue("FILL_IN_BLANK");
        ch3.createCell(6).setCellValue("education is essential");

        Row ch4 = childSheet.createRow(4);
        ch4.createCell(0).setCellValue("Match the words with their meanings.");
        ch4.createCell(1).setCellValue("MATCHING");
        ch4.createCell(7).setCellValue("abundant|evidence|climate");
        ch4.createCell(8).setCellValue("plentiful|proof|weather");
        ch4.createCell(9).setCellValue("1,2,3");

        Row ch5 = childSheet.createRow(5);
        ch5.createCell(0).setCellValue("Which of the following is TRUE according to the passage?");
        ch5.createCell(1).setCellValue("MULTIPLE_CHOICE_MULTI");
        ch5.createCell(2).setCellValue("All students passed the exam");
        ch5.createCell(3).setCellValue("Some students improved");
        ch5.createCell(4).setCellValue("The teacher was absent");
        ch5.createCell(5).setCellValue("The school closed");
        ch5.createCell(6).setCellValue("B");

        Row ch6 = childSheet.createRow(6);
        ch6.createCell(0).setCellValue("Summarize the author's opinion in 3 sentences.");
        ch6.createCell(1).setCellValue("WRITING");
        ch6.createCell(10).setCellValue("WRITING");
        ch6.createCell(11).setCellValue("B1");
    }

}
