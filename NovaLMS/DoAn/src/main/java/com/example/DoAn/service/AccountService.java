package com.example.DoAn.service;

import com.example.DoAn.dto.AccountDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccountService {
    Page<AccountDTO> getAllAccounts(Pageable pageable);
    List<AccountDTO> getAllAccounts();
    AccountDTO getAccountById(Integer id);
    void saveAccount(AccountDTO dto);
    void toggleStatus(Integer id);
}