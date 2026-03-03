package com.example.DoAn.controller;

import com.example.DoAn.dto.AccountDTO;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/list")
    public String viewList(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountDTO> userPage = accountService.getAllAccounts(pageable);
        var users = accountService.getAllAccounts();
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("users", users != null ? users : new java.util.ArrayList<AccountDTO>());
        model.addAttribute("pageTitle", "Quản lý tài khoản");
        model.addAttribute("isDashboard", true);
        return "admin/account-list";
    }

    @PostMapping("/toggle/{id}")
    public String toggleStatus(@PathVariable("id") Integer id) {
        accountService.toggleStatus(id);
        return "redirect:/admin/list";
    }

    @GetMapping("/accounts/add")
    public String showAddForm(Model model) {
        model.addAttribute("account", new AccountDTO());
        model.addAttribute("isDashboard", true);
        return "admin/account-form";
    }

    @PostMapping("/accounts/save")
    public String saveAccount(@ModelAttribute("account") AccountDTO accountDTO, RedirectAttributes ra) {
        try {
            accountService.saveAccount(accountDTO);
            ra.addFlashAttribute("message", "Tạo tài khoản mới thành công!");
            ra.addFlashAttribute("messageType", "success");
            return "redirect:/admin/list";
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi: " + e.getMessage());
            ra.addFlashAttribute("messageType", "danger");
            return "redirect:/admin/accounts/add";
        }
    }
    @GetMapping("/accounts/detail/{id}")
    public String viewDetail(@PathVariable("id") Integer id, Model model) {
        try {
            AccountDTO account = accountService.getAccountById(id);
            model.addAttribute("account", account);
            model.addAttribute("isDashboard", true);
            return "admin/account-details";
        } catch (Exception e) {
            return "redirect:/admin/list?error=notfound";
        }
    }
}