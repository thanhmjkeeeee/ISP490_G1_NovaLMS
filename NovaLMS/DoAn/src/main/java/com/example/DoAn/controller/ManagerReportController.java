package com.example.DoAn.controller;

import com.example.DoAn.model.Registration;
import com.example.DoAn.repository.RegistrationRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/reports")
@RequiredArgsConstructor
public class ManagerReportController {

    private final RegistrationRepository registrationRepository;

    @GetMapping("/export/registrations")
    public void exportRegistrations(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=registrations_report.csv");

        // Write BOM for Excel UTF-8 support
        response.getWriter().write('\ufeff');

        PrintWriter writer = response.getWriter();
        writer.println("ID,Học Viên,Email,Khóa Học,Lớp Học,Ngày Đăng Ký,Giá,Trạng Thái");

        List<Registration> registrations = registrationRepository.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Registration r : registrations) {
            writer.println(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,\"%s\"",
                r.getRegistrationId(),
                r.getUser() != null ? r.getUser().getFullName() : "N/A",
                r.getUser() != null ? r.getUser().getEmail() : "N/A",
                r.getCourse() != null ? r.getCourse().getTitle() : "N/A",
                r.getClazz() != null ? r.getClazz().getClassName() : "N/A",
                r.getRegistrationTime() != null ? r.getRegistrationTime().format(formatter) : "N/A",
                r.getRegistrationPrice() != null ? r.getRegistrationPrice() : 0.0,
                r.getStatus() != null ? r.getStatus() : "Pending"
            ));
        }
        writer.flush();
    }
}
