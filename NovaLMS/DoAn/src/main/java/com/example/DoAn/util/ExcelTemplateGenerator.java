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

}
