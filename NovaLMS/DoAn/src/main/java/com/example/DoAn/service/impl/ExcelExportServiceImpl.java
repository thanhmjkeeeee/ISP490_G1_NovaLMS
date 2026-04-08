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
        String[] columns = {"ID", "Mã Đơn Hàng", "Học Viên", "Email", "Khóa Học", "Lớp Học", "Số Tiền (VNĐ)", "Ngày Thanh Toán", "Trạng Thái"};

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
                Registration reg = registrationRepository.findById(payment.getRegistrationId()).orElse(null);

                row.createCell(0).setCellValue(payment.getId());
                row.createCell(1).setCellValue(payment.getPayosOrderCode() != null ? payment.getPayosOrderCode().toString() : "");
                
                if (reg != null) {
                    row.createCell(2).setCellValue(reg.getUser().getFullName());
                    row.createCell(3).setCellValue(reg.getUser().getEmail());
                    row.createCell(4).setCellValue(reg.getCourse().getCourseName());
                    row.createCell(5).setCellValue(reg.getClazz().getClassName());
                } else {
                    row.createCell(2).setCellValue("N/A");
                    row.createCell(3).setCellValue("N/A");
                    row.createCell(4).setCellValue("N/A");
                    row.createCell(5).setCellValue("N/A");
                }

                row.createCell(6).setCellValue(payment.getAmount().doubleValue());
                row.createCell(7).setCellValue(payment.getPaidAt() != null ? payment.getPaidAt().format(formatter) : "");
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
}
