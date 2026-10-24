package com.example.tlu_gpms_system.features.faculty.service.scope;

import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import com.example.tlu_gpms_system.features.faculty.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Tính phạm vi ngành của người dùng hiện tại.
 * Query DB ở mỗi lần gọi (không lưu scope trong JWT) để khi đổi Trưởng ngành thì người cũ mất quyền ngay.
 * Feature khác (account) gọi trực tiếp bean này, không inject MajorRepository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MajorScopeResolver {

    public static final String ACCESS_ALL_MAJORS = "ACCESS_ALL_MAJORS";

    private final MajorRepository majorRepository;

    public MajorScope current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        boolean accessAll = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ACCESS_ALL_MAJORS::equals);
        if (accessAll) {
            return MajorScope.allMajors();
        }

        String userId = authentication.getName();
        try {
            Long accountId = Long.valueOf(userId);
            return MajorScope.of(new HashSet<>(majorRepository.findIdsByHeadId(accountId)));
        } catch (NumberFormatException e) {
            log.error("Invalid user ID in authentication token: {}", userId);
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    // Ném MAJOR_NOT_IN_SCOPE nếu người dùng hiện tại không được thao tác trên ngành này
    public void requireAllows(Major major) {
        if (!current().allows(major)) {
            throw new AppException(ErrorCode.MAJOR_NOT_IN_SCOPE);
        }
    }
}
