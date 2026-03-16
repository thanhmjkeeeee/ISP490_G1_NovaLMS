package com.example.DoAn.service;

import com.example.DoAn.dto.request.AccountRequestDTO;
import com.example.DoAn.dto.response.AccountDetailResponse;
import com.example.DoAn.dto.response.PageResponse;

public interface AccountService {
    Integer saveAccount(AccountRequestDTO request);
    void updateAccount(Integer id, AccountRequestDTO request);
    void toggleStatus(Integer id);
    AccountDetailResponse getAccountById(Integer id);
    PageResponse<?> getAllAccounts(int pageNo, int pageSize, String search, Integer roleId, String status);
}