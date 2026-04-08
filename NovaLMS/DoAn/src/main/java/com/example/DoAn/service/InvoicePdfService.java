package com.example.DoAn.service;

import org.springframework.stereotype.Service;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Service sinh PDF hóa đơn thanh toán khóa học.
 * Sử dụng thư viện OpenPDF (com.github.librepdf:openpdf).
 */
@Service
public class InvoicePdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 80, 140));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(50, 50, 50));
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(30, 30, 30));
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(30, 30, 30));
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(100, 100, 100));
    private static final Font RED_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(200, 30, 30));

    /**
     * Sinh PDF hóa đơn thanh toán.
     *
     * @param studentName  tên học viên
     * @param studentEmail email học viên
     * @param courseName   tên khóa học
     * @param className    tên lớp học
     * @param amount       số tiền thanh toán (VD: "150000")
     * @param paymentId    mã thanh toán nội bộ (ID của bản ghi Payment)
     * @param orderCode    mã đơn PayOS
     * @param paidAt       thời gian thanh toán thành công (định dạng "yyyy-MM-dd HH:mm:ss")
     * @return mảng byte của file PDF
     */
    public byte[] generateInvoice(
            String studentName,
            String studentEmail,
            String courseName,
            String className,
            String amount,
            String paymentId,
            String orderCode,
            String paidAt
    ) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            // ── HEADER ──────────────────────────────────────────────────────
            addHeader(document);

            // ── TITLE ───────────────────────────────────────────────────────
            addTitle(document);

            // ── INVOICE INFO ───────────────────────────────────────────────
            addInvoiceInfo(document, paymentId, paidAt, orderCode);

            // ── Divider ────────────────────────────────────────────────────
            addDivider(document);

            // ── STUDENT INFO ───────────────────────────────────────────────
            addSectionTitle(document, "THONG TIN HOC VIEN");
            addStudentInfo(document, studentName, studentEmail);

            // ── COURSE INFO ────────────────────────────────────────────────
            addSectionTitle(document, "THONG TIN KHOA HOC");
            addCourseInfo(document, courseName, className);

            // ── Divider ────────────────────────────────────────────────────
            addDivider(document);

            // ── PAYMENT DETAILS ────────────────────────────────────────────
            addPaymentDetails(document, amount, paymentId, orderCode, paidAt);

            // ── FOOTER ────────────────────────────────────────────────────
            addFooter(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Loi khi sinh PDF hoa don: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{3f, 1f});

        // Logo / Brand column
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setPaddingBottom(10);

        Paragraph brand = new Paragraph();
        brand.setFont(new Font(Font.HELVETICA, 22, Font.BOLD, new Color(30, 100, 180)));
        brand.add("NovaLMS");
        brandCell.addElement(brand);

        Paragraph tagline = new Paragraph();
        tagline.setFont(SMALL_FONT);
        tagline.add("Hoc tieng Anh - Tao tuong lai");
        brandCell.addElement(tagline);

        // Date column
        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setPaddingBottom(10);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String now = sdf.format(new Date());
        Paragraph dateLine = new Paragraph("Ngay in: " + now, SMALL_FONT);
        dateCell.addElement(dateLine);

        headerTable.addCell(brandCell);
        headerTable.addCell(dateCell);
        document.add(headerTable);
    }

    private void addTitle(Document document) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(10);
        document.add(spacer);

        Paragraph title = new Paragraph();
        title.setFont(TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.add("HOA DON THANH TOAN");
        document.add(title);

        Paragraph sub = new Paragraph();
        sub.setFont(SMALL_FONT);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.add("Payment Invoice");
        sub.setSpacingAfter(5);
        document.add(sub);
    }

    private void addInvoiceInfo(Document document, String paymentId, String paidAt, String orderCode) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(3);
        infoTable.setWidthPercentage(100f);
        infoTable.setWidths(new float[]{1f, 1f, 1f});
        infoTable.setSpacingBefore(15);

        addInfoCell(infoTable, "Ma Hoa Don", paymentId != null ? "#" + paymentId : "-");
        addInfoCell(infoTable, "Ma Don PayOS", orderCode != null ? orderCode : "-");
        addInfoCell(infoTable, "Ngay Thanh Toan", paidAt != null ? paidAt : "-");

        document.add(infoTable);
    }

    private void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(200, 200, 200));
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(245, 247, 252));

        Paragraph p = new Paragraph();
        p.setFont(SMALL_FONT);
        p.add(label);
        p.setSpacingAfter(3);
        p.setAlignment(Element.ALIGN_CENTER);

        Paragraph v = new Paragraph();
        v.setFont(HEADER_FONT);
        v.add(nullToEmpty(value));
        v.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(p);
        cell.addElement(v);
        table.addCell(cell);
    }

    private void addDivider(Document document) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(10);
        spacer.setSpacingAfter(10);
        document.add(spacer);

        LineSeparator line = new LineSeparator();
        line.setOffset(2);
        line.setLineColor(new Color(200, 200, 200));
        document.add(line);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setFont(HEADER_FONT);
        p.setSpacingBefore(8);
        p.setSpacingAfter(5);
        p.add(title);
        document.add(p);
    }

    private void addStudentInfo(Document document, String studentName, String studentEmail) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{1f, 2f});
        table.setSpacingBefore(5);

        addLabelCell(table, "Ho va Ten:");
        addValueCell(table, nullToEmpty(studentName));
        addLabelCell(table, "Email:");
        addValueCell(table, nullToEmpty(studentEmail));

        document.add(table);
    }

    private void addCourseInfo(Document document, String courseName, String className) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{1f, 2f});
        table.setSpacingBefore(5);

        addLabelCell(table, "Khoa hoc:");
        addValueCell(table, nullToEmpty(courseName));
        addLabelCell(table, "Lop hoc:");
        addValueCell(table, nullToEmpty(className));

        document.add(table);
    }

    private void addPaymentDetails(Document document, String amount, String paymentId, String orderCode, String paidAt) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{1f, 2f});
        table.setSpacingBefore(5);

        addLabelCell(table, "So tien:");
        addValueCell(table, formatAmount(amount) + " VND");

        addLabelCell(table, "Phuong thuc:");
        addValueCell(table, "PayOS (VNPAY)");

        addLabelCell(table, "Ma thanh toan:");
        addValueCell(table, "#" + nullToEmpty(paymentId));

        addLabelCell(table, "Ma don PayOS:");
        addValueCell(table, nullToEmpty(orderCode));

        addLabelCell(table, "Thoi gian:");
        addValueCell(table, nullToEmpty(paidAt));

        document.add(table);

        // Total amount box
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(10);
        document.add(spacer);

        PdfPTable totalTable = new PdfPTable(1);
        totalTable.setWidthPercentage(50f);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell totalCell = new PdfPCell();
        totalCell.setBorder(Rectangle.BOX);
        totalCell.setBorderColor(new Color(30, 100, 180));
        totalCell.setBackgroundColor(new Color(235, 245, 255));
        totalCell.setPadding(12);

        Paragraph label = new Paragraph();
        label.setFont(NORMAL_FONT);
        label.add("TONG CONG");
        label.setAlignment(Element.ALIGN_CENTER);
        label.setSpacingAfter(5);

        Paragraph value = new Paragraph();
        value.setFont(RED_FONT);
        value.add(formatAmount(amount) + " VND");
        value.setAlignment(Element.ALIGN_CENTER);

        totalCell.addElement(label);
        totalCell.addElement(value);
        totalTable.addCell(totalCell);
        document.add(totalTable);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(30);
        spacer.setSpacingAfter(5);
        document.add(spacer);

        LineSeparator line = new LineSeparator();
        line.setLineColor(new Color(200, 200, 200));
        document.add(line);

        Paragraph footer = new Paragraph();
        footer.setFont(SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        footer.add("NovaLMS - He thong quan ly hoc tap truc tuyen\n");
        footer.add("Website: http://localhost:8080  |  Email: support@novalms.com\n");
        footer.add("Hoa don nay duoc tao tu dong boi he thong NovaLMS.");
        document.add(footer);
    }

    private void addLabelCell(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell(new Phrase(label, BOLD_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(5);
        cell.setPaddingTop(3);
        table.addCell(cell);
    }

    private void addValueCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(5);
        cell.setPaddingTop(3);
        table.addCell(cell);
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private String formatAmount(String amount) {
        if (amount == null || amount.isBlank()) return "0";
        try {
            long val = Long.parseLong(amount.trim().replace(",", ""));
            return NumberFormat.getNumberInstance(Locale.US).format(val);
        } catch (NumberFormatException e) {
            return amount;
        }
    }
}
