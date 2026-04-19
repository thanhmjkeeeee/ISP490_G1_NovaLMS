package com.example.DoAn.controller;

import com.example.DoAn.service.IExpertModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/expert")
@RequiredArgsConstructor
public class ExpertViewController {

    private final IExpertModuleService moduleService;
    private final com.example.DoAn.service.ICourseService courseService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("isDashboard", true);
        return "expert/dashboard";
    }

    // ── Module Management ─────────────────────────────────────────────────

    @GetMapping("/modules")
    public String modulesPage(Model model, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return "redirect:/login.html";

        List<?> coursesResult = moduleService.getCoursesOwnedByExpert(email);
        model.addAttribute("courses", coursesResult);
        model.addAttribute("isDashboard", true);
        return "expert/module-list";
    }

    // ── Content Management ─────────────────────────────────────────────────

    @GetMapping("/content")
    public String contentPage(Model model, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return "redirect:/login.html";

        List<?> coursesResult = moduleService.getCoursesOwnedByExpert(email);
        model.addAttribute("courses", coursesResult);
        model.addAttribute("isDashboard", true);
        return "expert/content";
    }

    @GetMapping({"/content/course", "/content/course/"})
    public String contentRedirect() {
        return "redirect:/expert/content";
    }

    @GetMapping("/content/course/{courseId}")
    public String courseContentPage(@PathVariable Integer courseId, Model model, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return "redirect:/login.html";

        var course = courseService.getById(courseId);
        List<?> modulesResult = moduleService.getModulesByCourse(courseId, email);
        
        model.addAttribute("courseId", courseId);
        model.addAttribute("course", course);
        model.addAttribute("modules", modulesResult);
        model.addAttribute("isDashboard", true);
        return "expert/content-course";
    }

    // ── Quiz Bank (Legacy) ───────────────────────────────────────────────

    @GetMapping("/quiz-bank")
    public String quizBankPage(Model model, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return "redirect:/login.html";

        List<?> coursesResult = moduleService.getCoursesOwnedByExpert(email);
        model.addAttribute("courses", coursesResult);
        model.addAttribute("isDashboard", true);
        return "expert/quiz-bank";
    }

    @GetMapping({"/quiz-bank/course", "/quiz-bank/course/"})
    public String quizBankRedirect() {
        return "redirect:/expert/quiz-bank";
    }

    @GetMapping("/quiz-bank/course/{courseId}")
    public String courseQuizBankPage(@PathVariable Integer courseId, Model model, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return "redirect:/login.html";

        List<?> modulesResult = moduleService.getModulesByCourse(courseId, email);
        model.addAttribute("courseId", courseId);
        model.addAttribute("modules", modulesResult);
        model.addAttribute("isDashboard", true);
        return "expert/quiz-bank-course";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  QUESTION BANK (Quản lý ngân hàng đề)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/question-bank")
    public String questionBankPage(Model model) {
        model.addAttribute("isDashboard", true);
        return "expert/question-bank";
    }

    @GetMapping("/question-bank/create")
    public String questionCreatePage(Model model) {
        model.addAttribute("isDashboard", true);
        return "expert/question-create";
    }

    @GetMapping("/question-bank/{itemId}/edit")
    public String questionEditPage(@PathVariable Integer itemId, @RequestParam(required = false, defaultValue = "SINGLE") String type, Model model) {
        model.addAttribute("isDashboard", true);
        model.addAttribute("itemId", itemId);
        model.addAttribute("itemType", type);
        return "expert/question-edit";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  AI QUESTION GENERATION (MODAL SPLIT INTO PAGES)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/question-bank/ai-quick")
    public String aiQuickPage(Model model) {
        model.addAttribute("isDashboard", true);
        return "expert/ai-quick";
    }

    @GetMapping("/question-bank/ai-module")
    public String aiModulePage(Model model, Principal principal) {
        String email = getEmail(principal);
        if (email != null) {
            model.addAttribute("courses", moduleService.getCoursesOwnedByExpert(email));
        }
        model.addAttribute("isDashboard", true);
        return "expert/ai-module";
    }

    @GetMapping("/question-bank/ai-group")
    public String aiGroupPage(Model model) {
        model.addAttribute("isDashboard", true);
        return "expert/ai-group";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  QUESTION APPROVAL QUEUE (SPEC 003)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/question-approval")
    public String questionApprovalPage(Model model) {
        model.addAttribute("isDashboard", true);
        return "expert/question-approval";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  QUIZ MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/quiz-management")
    public String quizListPage(Model model, Principal principal) {
        String email = getEmail(principal);
        if (email != null) {
            model.addAttribute("courses", moduleService.getCoursesOwnedByExpert(email));
        }
        model.addAttribute("isDashboard", true);
        model.addAttribute("mode", "QUIZ");
        return "expert/quiz-list";
    }

    @GetMapping("/quiz-management/create")
    public String quizCreatePage(
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(required = false) Integer quizId,
            Model model) {
        model.addAttribute("isDashboard", true);
        model.addAttribute("moduleId", moduleId);
        model.addAttribute("quizId", quizId);
        return "expert/quiz-create";
    }

    @GetMapping("/quiz-management/{quizId}/edit")
    public String quizEditPage(@PathVariable Integer quizId, Model model) {
        model.addAttribute("isDashboard", true);
        model.addAttribute("quizId", quizId);
        return "expert/quiz-edit";
    }

    @GetMapping("/quiz-management/{quizId}/questions")
    public String quizQuestionsPage(@PathVariable Integer quizId, Model model) {
        model.addAttribute("isDashboard", true);
        model.addAttribute("quizId", quizId);
        return "expert/quiz-questions";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ASSIGNMENT MANAGEMENT (redirect to unified quiz list)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/assignment-management")
    public String assignmentListPage(Model model, Principal principal) {
        String email = getEmail(principal);
        if (email != null) {
            model.addAttribute("courses", moduleService.getCoursesOwnedByExpert(email));
        }
        model.addAttribute("isDashboard", true);
        model.addAttribute("mode", "ASSIGNMENT");
        return "expert/quiz-list";
    }
}
