package com.example.DoAn.service.impl;

import com.example.DoAn.dto.AccountDTO;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return userRepository.findAllUsersWithRole(Pageable.unpaged()).getContent().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Integer id) {
        return userRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @Override
    public void saveAccount(AccountDTO dto) {
        User user = (dto.getUserId() != null)
                ? userRepository.findById(dto.getUserId()).orElse(new User())
                : new User();

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());

        if (dto.getRoleId() != null) {
            user.setRole(settingRepository.findById(dto.getRoleId()).orElse(null));
        }

        if (user.getUserId() == null) {
            user.setStatus("Active");
            user.setPassword("default_password");
        }
        userRepository.save(user);
    }

    @Override
    public void toggleStatus(Integer id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setStatus("Active".equalsIgnoreCase(user.getStatus()) ? "Inactive" : "Active");
        userRepository.save(user);
    }

    private AccountDTO mapToDTO(User user) {
        return AccountDTO.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .roleId(user.getRole() != null ? user.getRole().getSettingId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : "Chưa có Role")
                .status(user.getStatus() != null ? user.getStatus() : "Inactive")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountDTO> getAllAccounts(Pageable pageable) {
        return userRepository.findAllUsersWithRole(pageable)
                .map(this::mapToDTO);
    }
}