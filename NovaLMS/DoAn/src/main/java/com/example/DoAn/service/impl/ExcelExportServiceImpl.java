package com.example.DoAn.service.impl;

import com.example.DoAn.model.Payment;
import com.example.DoAn.model.Registration;
import com.example.DoAn.repository.PaymentRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportServiceImpl implements ExcelExportService {

    private final PaymentRepository paymentRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    public ByteArrayInputStream exportRevenueToExcel() {
        String[] columns = { "ID", "Mã Đơn Hàng", "Học Viên", "Email", "Khóa Học", "Lớp Học", "Số Tiền (VNĐ)",
                "Ngày Thanh Toán", "Trạng Thái" };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo Cáo Doanh Thu");

            // Header Font & Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Data
            List<Payment> payments = paymentRepository.findByStatusOrderByPaidAtDesc("PAID");
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            for (Payment payment : payments) {
                Row row = sheet.createRow(rowIdx++);

                // Lấy chi tiết registration để có thông tin học viên & khóa học
                Registration enrollment = registrationRepository.findWithAssociationsById(payment.getRegistrationId())
                        .orElse(null);

                row.createCell(0).setCellValue(payment.getId());
                row.createCell(1).setCellValue(
                        payment.getPayosOrderCode() != null ? payment.getPayosOrderCode().toString() : "");

                if (enrollment != null) {
                    row.createCell(2).setCellValue(enrollment.getUser().getFullName());
                    row.createCell(3).setCellValue(enrollment.getUser().getEmail());
                    row.createCell(4).setCellValue(enrollment.getCourse().getCourseName());
                    row.createCell(5).setCellValue(enrollment.getClazz() != null ? enrollment.getClazz().getClassName() : "Học tự do");
                } else {
                    row.createCell(2).setCellValue("N/A");
                    row.createCell(3).setCellValue("N/A");
                    row.createCell(4).setCellValue("N/A");
                    row.createCell(5).setCellValue("N/A");
                }

                row.createCell(6).setCellValue(payment.getAmount().doubleValue());
                row.createCell(7)
                        .setCellValue(payment.getPaidAt() != null ? payment.getPaidAt().format(formatter) : "");
                row.createCell(8).setCellValue(payment.getStatus());
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            log.error("Excel Export Error: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi xuất file Excel: " + e.getMessage());

        }
    }

    @Override
    public ByteArrayInputStream exportRegistrationsToExcel() {
        String[] columns = { "ID", "Học Viên", "Email", "Khóa Học", "Lớp Học", "Giá Đăng Ký", "Ngày Đăng Ký",
                "Trạng Thái", "Ghi Chú" };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh Sách Đăng Ký");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            List<Registration> registrations = registrationRepository.findAllRegistrations();
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Registration r : registrations) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(r.getRegistrationId());
                row.createCell(1).setCellValue(r.getUser() != null ? r.getUser().getFullName() : "N/A");
                row.createCell(2).setCellValue(r.getUser() != null ? r.getUser().getEmail() : "N/A");
                row.createCell(3).setCellValue(r.getCourse() != null ? r.getCourse().getCourseName() : "N/A");
                row.createCell(4).setCellValue(r.getClazz() != null ? r.getClazz().getClassName() : "Học tự do");
                row.createCell(5)
                        .setCellValue(r.getRegistrationPrice() != null ? r.getRegistrationPrice().doubleValue() : 0.0);
                row.createCell(6)
                        .setCellValue(r.getRegistrationTime() != null ? r.getRegistrationTime().format(formatter) : "");
                row.createCell(7).setCellValue(r.getStatus() != null ? r.getStatus() : "Pending");
                row.createCell(8).setCellValue(r.getNote() != null ? r.getNote() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            log.error("Excel Export Registrations Error: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi xuất file Excel danh sách đăng ký: " + e.getMessage());
        }
    }
}
