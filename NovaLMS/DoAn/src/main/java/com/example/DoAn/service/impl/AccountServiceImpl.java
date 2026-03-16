package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AccountRequestDTO;
import com.example.DoAn.dto.response.AccountDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.AccountService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
                .password("default123")
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
    public PageResponse<AccountDetailResponse> getAllAccounts(int pageNo, int pageSize, String search, Integer roleId, String status) {
        PageRequest pageable = PageRequest.of(pageNo, pageSize);

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by fullName or email
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim() + "%";
                Predicate fullNamePredicate = cb.like(cb.lower(root.get("fullName")), searchPattern.toLowerCase());
                Predicate emailPredicate = cb.like(cb.lower(root.get("email")), searchPattern.toLowerCase());
                predicates.add(cb.or(fullNamePredicate, emailPredicate));
            }

            // Filter by roleId
            if (roleId != null) {
                predicates.add(cb.equal(root.get("role").get("settingId"), roleId));
            }

            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> page = userRepository.findAll(spec, pageable);

        List<AccountDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<AccountDetailResponse>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages())
                .items(list)
                .build();
    }

    private AccountDetailResponse mapToResponse(User user) {
        return AccountDetailResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .roleId(user.getRole() != null ? user.getRole().getSettingId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : "N/A")
                .status(user.getStatus())
                .build();
    }
}