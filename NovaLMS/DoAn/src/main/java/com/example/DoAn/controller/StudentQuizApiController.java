package com.example.DoAn.controller;

import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResultHistoryDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/student/quiz")
@RequiredArgsConstructor
public class StudentQuizApiController {

    private final QuizResultService quizResultService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/history")
    public ResponseData<PageResponse<QuizResultHistoryDTO>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            PageResponse<QuizResultHistoryDTO> res = quizResultService.getStudentQuizHistory(email, page, size, category, keyword);
            return ResponseData.success("Thành công", res);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }
}
