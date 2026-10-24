package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.common.enums.Role;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.RoleRepository;
import com.example.tlu_gpms_system.features.account.service.AccountRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Chỉ được gọi nội bộ từ Service của feature khác, nên không đặt @PreAuthorize:
// method public của Service gọi tới đã kiểm tra quyền.
// Role nằm trong JWT nên tài khoản phải đăng nhập lại mới nhận role mới
@Service
@RequiredArgsConstructor
public class AccountRoleServiceImpl implements AccountRoleService {
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantRole(Long accountId, Role role) {
        Account account = findAccount(accountId);
        boolean hasRole = account.getRoles().stream().anyMatch(r -> role.name().equals(r.getName()));
        if (hasRole) {
            return;
        }
        account.getRoles().add(roleRepository.findById(role.name())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST)));
        accountRepository.save(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeRole(Long accountId, Role role) {
        Account account = findAccount(accountId);
        if (account.getRoles().removeIf(r -> role.name().equals(r.getName()))) {
            accountRepository.save(account);
        }
    }

    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
