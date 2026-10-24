package com.example.tlu_gpms_system.configuration.security;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Role;
import com.example.tlu_gpms_system.features.account.repository.RoleRepository;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationInitConfig implements ApplicationRunner {
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!accountRepository.existsByEmail("tlugpmssystem@gmail.com")) {
            List<Role> role = roleRepository.findAllById(List.of(com.example.tlu_gpms_system.common.enums.Role.ADMIN.name()));
            Account user = Account.builder()
                    .email("tlugpmssystem@gmail.com")
                    .status(true)
                    .firstName("Ad")
                    .lastName("min")
                    .phone("031234567")
                    .roles(new HashSet<>(role))
                    .build();
            accountRepository.save(user);
        }
    }
}
