package com.example.DoAn.controller;

import com.example.DoAn.dto.request.AccountRequestDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
@Slf4j
@Tag(name = "Account Controller")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Add new account")
    @PostMapping("/")
    public ResponseData<Integer> addAccount(@Valid @RequestBody AccountRequestDTO request) {
        log.info("Adding new account: {}", request.getEmail());
        try {
            Integer userId = accountService.saveAccount(request);
            return new ResponseData<>(HttpStatus.CREATED.value(), "Success", userId);
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Update account")
    @PutMapping("/{id}")
    public ResponseData<Void> updateAccount(@PathVariable Integer id, @Valid @RequestBody AccountRequestDTO request) {
        try {
            accountService.updateAccount(id, request);
            return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Updated");
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Get list account")
    @GetMapping("/list")
    public ResponseData<PageResponse<?>> getList(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            PageResponse<?> response = accountService.getAllAccounts(pageNo, pageSize);
            return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Toggle account status")
    @PatchMapping("/{id}/status")
    public ResponseData<Void> toggleStatus(@PathVariable Integer id) {
        try {
            accountService.toggleStatus(id);
            return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Status changed");
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
}