package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AccountRequestDTO;
import com.example.DoAn.dto.response.AccountDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;

    @Override
    public Integer saveAccount(AccountRequestDTO request) {
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .role(settingRepository.findById(request.getRoleId()).orElse(null))
                .status("Active")
                .password("default123") // Giả định mật khẩu mặc định
                .build();

        userRepository.save(user);
        log.info("User saved successfully, userId={}", user.getUserId());
        return user.getUserId();
    }

    @Override
    public void updateAccount(Integer id, AccountRequestDTO request) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        user.setFullName(request.getFullName());
        user.setMobile(request.getMobile());
        user.setRole(settingRepository.findById(request.getRoleId()).orElse(null));
        if(request.getStatus() != null) user.setStatus(request.getStatus());

        userRepository.save(user);
        log.info("User updated successfully, userId={}", id);
    }

    @Override
    public void toggleStatus(Integer id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        String newStatus = "Active".equalsIgnoreCase(user.getStatus()) ? "Inactive" : "Active";
        userRepository.updateStatusNative(newStatus, id);
        log.info("User status toggled to {}, userId={}", newStatus, id);
    }

    @Override
    public AccountDetailResponse getAccountById(Integer id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        return mapToResponse(user);
    }

    @Override
    public PageResponse<?> getAllAccounts(int pageNo, int pageSize) {
        Page<User> page = userRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<AccountDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPage(page.getTotalPages())
                .items(list)
                .build();
    }

    private AccountDetailResponse mapToResponse(User user) {
        return AccountDetailResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .roleName(user.getRole() != null ? user.getRole().getName() : "N/A")
                .status(user.getStatus())
                .build();
    }
}