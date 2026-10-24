package com.example.tlu_gpms_system.features.faculty.service.scope;

import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import com.example.tlu_gpms_system.features.faculty.repository.MajorRepository;
import com.example.tlu_gpms_system.testreport.UnitTestCase;
import com.example.tlu_gpms_system.testreport.UnitTestCase.Type;
import com.example.tlu_gpms_system.testreport.UtcReportExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, UtcReportExtension.class})
class MajorScopeResolverTest {

    @Mock
    private MajorRepository majorRepository;

    @InjectMocks
    private MajorScopeResolver majorScopeResolver;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ===================== Helper =====================

    // Đăng nhập giả lập: name = account.id, authorities giống claim trong JWT
    private static void login(String accountId, String... authorities) {
        List<SimpleGrantedAuthority> granted = Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        Authentication authentication = new UsernamePasswordAuthenticationToken(accountId, null, granted);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Major major(Long id) {
        return Major.builder().id(id).code("MAJOR" + id).name("Ngành " + id).build();
    }

    // ===================== current =====================

    @Test
    @UnitTestCase(id = "UTC001", method = "current", type = Type.NORMAL,
            precondition = "Người dùng đăng nhập có authority ROLE_ADMIN, ACCESS_ALL_MAJORS",
            input = "Không có",
            returns = "MajorScope(all = true); allows mọi ngành; không query majorRepository")
    void UTC001_current_accessAllMajors_returnAllScope() {
        login("1", "ROLE_ADMIN", "ACCESS_ALL_MAJORS");

        MajorScope scope = majorScopeResolver.current();

        assertThat(scope.all()).isTrue();
        assertThat(scope.isEmpty()).isFalse();
        assertThat(scope.allows(major(99L))).isTrue();
        verify(majorRepository, never()).findIdsByHeadId(any());
    }

    @Test
    @UnitTestCase(id = "UTC002", method = "current", type = Type.NORMAL,
            precondition = "Người dùng id = 5 có ROLE_HEAD_OF_MAJOR; majorRepository.findIdsByHeadId(5) trả về [1]",
            input = "Không có",
            returns = "MajorScope(all = false, majorIds = [1]); allows ngành 1, không allows ngành 2")
    void UTC002_current_headOfMajor_returnOwnMajor() {
        login("5", "ROLE_HEAD_OF_MAJOR", "IMPORT_STUDENT");
        when(majorRepository.findIdsByHeadId(5L)).thenReturn(List.of(1L));

        MajorScope scope = majorScopeResolver.current();

        assertThat(scope.all()).isFalse();
        assertThat(scope.majorIds()).containsExactly(1L);
        assertThat(scope.allows(major(1L))).isTrue();
        assertThat(scope.allows(major(2L))).isFalse();
    }

    @Test
    @UnitTestCase(id = "UTC003", method = "current", type = Type.BOUNDARY,
            precondition = "Người dùng id = 7 có ROLE_HEAD_OF_MAJOR nhưng đã bị gỡ khỏi ngành; findIdsByHeadId(7) trả về []",
            input = "Không có",
            returns = "MajorScope rỗng: isEmpty() = true, không allows ngành nào")
    void UTC003_current_headRemovedFromMajor_returnEmptyScope() {
        login("7", "ROLE_HEAD_OF_MAJOR");
        when(majorRepository.findIdsByHeadId(7L)).thenReturn(List.of());

        MajorScope scope = majorScopeResolver.current();

        assertThat(scope.isEmpty()).isTrue();
        assertThat(scope.allows(major(1L))).isFalse();
    }

    @Test
    @UnitTestCase(id = "UTC004", method = "current", type = Type.ABNORMAL,
            precondition = "SecurityContext không có Authentication",
            input = "Không có",
            exception = "AppException: UNAUTHORIZED")
    void UTC004_current_noAuthentication_throwUnauthorized() {
        assertThatThrownBy(() -> majorScopeResolver.current())
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @UnitTestCase(id = "UTC005", method = "current", type = Type.ABNORMAL,
            precondition = "SecurityContext chứa AnonymousAuthenticationToken",
            input = "Không có",
            exception = "AppException: UNAUTHORIZED")
    void UTC005_current_anonymous_throwUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> majorScopeResolver.current())
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @UnitTestCase(id = "UTC006", method = "current", type = Type.ABNORMAL,
            precondition = "Người dùng đăng nhập với name = \"abc\" (sub không phải số), không có ACCESS_ALL_MAJORS",
            input = "Không có",
            exception = "AppException: TOKEN_INVALID")
    void UTC006_current_subNotNumber_throwTokenInvalid() {
        login("abc", "ROLE_HEAD_OF_MAJOR");

        assertThatThrownBy(() -> majorScopeResolver.current())
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    // ===================== requireAllows =====================

    @Test
    @UnitTestCase(id = "UTC007", method = "requireAllows", type = Type.ABNORMAL,
            precondition = "Người dùng id = 5 là Trưởng ngành 1 (findIdsByHeadId(5) trả về [1])",
            input = "major = ngành id 2",
            exception = "AppException: MAJOR_NOT_IN_SCOPE")
    void UTC007_requireAllows_otherMajor_throwMajorNotInScope() {
        login("5", "ROLE_HEAD_OF_MAJOR");
        when(majorRepository.findIdsByHeadId(5L)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> majorScopeResolver.requireAllows(major(2L)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MAJOR_NOT_IN_SCOPE);
    }

    @Test
    @UnitTestCase(id = "UTC008", method = "requireAllows", type = Type.BOUNDARY,
            precondition = "Người dùng id = 5 là Trưởng ngành 1 (findIdsByHeadId(5) trả về [1])",
            input = "major = null (ví dụ giáo viên ngoài trường không có ngành)",
            exception = "AppException: MAJOR_NOT_IN_SCOPE")
    void UTC008_requireAllows_nullMajorForHead_throwMajorNotInScope() {
        login("5", "ROLE_HEAD_OF_MAJOR");
        when(majorRepository.findIdsByHeadId(5L)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> majorScopeResolver.requireAllows(null))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MAJOR_NOT_IN_SCOPE);
    }

    @Test
    @UnitTestCase(id = "UTC009", method = "requireAllows", type = Type.NORMAL,
            precondition = "Người dùng có ACCESS_ALL_MAJORS",
            input = "major = null",
            returns = "Không ném exception (Admin được thao tác cả dữ liệu không có ngành)")
    void UTC009_requireAllows_adminNullMajor_noException() {
        login("1", "ROLE_ADMIN", "ACCESS_ALL_MAJORS");

        majorScopeResolver.requireAllows(null);

        verifyNoInteractions(majorRepository);
    }
}
