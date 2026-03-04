package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/admin")
public class AccountViewController {

    @GetMapping("/list")
    public String listPage() {
        return "admin/account-list";
    }

    @GetMapping("/accounts/add")
    public String addPage() {
        return "admin/account-form";
    }

    @GetMapping("/accounts/detail/{id}")
    public String detailPage(@PathVariable Integer id) {
        return "admin/account-details";
    }
}