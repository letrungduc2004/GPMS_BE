package com.example.tlu_gpms_system;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationFlowTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private AccountRepository accountRepository;

        @Autowired
        private AuthenticationService authenticationService;

        @Test
        @DisplayName("Kiểm tra toàn bộ luồng xác thực sau đăng nhập và refresh-token")
        void testAuthenticationFlow() throws Exception {
                Account account = accountRepository.findByEmailAndStatus("tlugpmssystem@gmail.com")
                                .orElseThrow();
                String token = authenticationService.createToken(account);
                Cookie cookie = new Cookie("access_token", token);

                // 1. GET /my-info có cookie -> 200 code 1000
                mockMvc.perform(get("/my-info").cookie(cookie))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(1000))
                                .andExpect(jsonPath("$.data.roles").isArray());

                // 2. GET /my-info KHÔNG cookie -> 401 code 2000
                mockMvc.perform(get("/my-info"))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value(2000));

                // 3. GET /notification có cookie -> 200 code 1000
                mockMvc.perform(get("/notification").cookie(cookie))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(1000))
                                .andExpect(jsonPath("$.data").isArray());

                // 4. GET /connection KHÔNG cookie -> 401
                mockMvc.perform(get("/connection"))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());

                // 5. GET /connection CÓ cookie -> 200
                mockMvc.perform(get("/connection").cookie(cookie))
                                .andDo(print())
                                .andExpect(status().isOk());

                // 6. POST /refresh-token với cookie -> 200 code 1000, trả Set-Cookie mới
                MvcResult refreshResult = mockMvc.perform(post("/refresh-token").cookie(cookie))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(1000))
                                .andReturn();

                String setCookieHeader = refreshResult.getResponse().getHeader("Set-Cookie");
                assertThat(setCookieHeader).isNotNull();
                assertThat(setCookieHeader).contains("access_token=");

                // 7. POST /log-out với cookie -> 200 code 1000, Set-Cookie xóa access_token và refresh_token với Max-Age=0
                MvcResult logoutResult = mockMvc.perform(post("/log-out").cookie(cookie))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(1000))
                                .andReturn();

                java.util.List<String> logoutCookies = logoutResult.getResponse().getHeaders("Set-Cookie");
                assertThat(logoutCookies).isNotEmpty();
                assertThat(logoutCookies.stream().anyMatch(c -> c.contains("access_token=") && c.contains("Max-Age=0"))).isTrue();
                assertThat(logoutCookies.stream().anyMatch(c -> c.contains("refresh_token=") && c.contains("Max-Age=0"))).isTrue();

                // 8. Ngay sau khi logout, gọi GET /my-info với token cũ -> 401 (do token đã vào blacklist)
                mockMvc.perform(get("/my-info").cookie(cookie))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value(2000));
        }
}
