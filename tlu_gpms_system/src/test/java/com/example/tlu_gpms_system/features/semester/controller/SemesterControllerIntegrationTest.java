package com.example.tlu_gpms_system.features.semester.controller;

import com.example.tlu_gpms_system.common.enums.SemesterStatus;
import com.example.tlu_gpms_system.features.semester.dto.request.SemesterRequest;
import com.example.tlu_gpms_system.features.semester.entity.Semester;
import com.example.tlu_gpms_system.features.semester.repository.SemesterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SemesterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SemesterRepository semesterRepository;

    @AfterEach
    void tearDown() {
        semesterRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /semester - Trả về 200 khi ADMIN lấy danh sách học kỳ")
    @WithMockUser(roles = "ADMIN")
    void getAll_whenAdmin_shouldReturn200() throws Exception {
        Semester semester = Semester.builder()
                .name("Học kỳ 1 - 2026")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 1, 31))
                .status(SemesterStatus.UPCOMING)
                .build();
        semesterRepository.save(semester);

        mockMvc.perform(get("/semester")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Học kỳ 1 - 2026")));
    }

    @Test
    @DisplayName("GET /semester - Trả về 403 khi STUDENT gọi API")
    @WithMockUser(roles = "STUDENT")
    void getAll_whenStudent_shouldReturn403() throws Exception {
        mockMvc.perform(get("/semester")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /semester - Trả về 200 khi thêm mới học kỳ thành công")
    @WithMockUser(roles = "ADMIN")
    void create_whenValidRequest_shouldReturn200() throws Exception {
        SemesterRequest request = SemesterRequest.builder()
                .name("Học kỳ 2 - 2026")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        mockMvc.perform(post("/semester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Thêm mới học kỳ thành công")));
    }

    @Test
    @DisplayName("POST /semester - Trả về 400 khi tên học kỳ trống")
    @WithMockUser(roles = "ADMIN")
    void create_whenNameEmpty_shouldReturn400() throws Exception {
        SemesterRequest request = SemesterRequest.builder()
                .name("")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        mockMvc.perform(post("/semester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(4000)))
                .andExpect(jsonPath("$.message", is("Tên học kỳ không được để trống")));
    }

    @Test
    @DisplayName("PUT /semester/{id} - Trả về 200 khi cập nhật học kỳ thành công")
    @WithMockUser(roles = "ADMIN")
    void update_whenValidRequest_shouldReturn200() throws Exception {
        Semester semester = Semester.builder()
                .name("Học kỳ Hè - 2026")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status(SemesterStatus.UPCOMING)
                .build();
        Semester saved = semesterRepository.save(semester);

        SemesterRequest request = SemesterRequest.builder()
                .name("Học kỳ Phụ - 2026")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status(SemesterStatus.ONGOING)
                .build();

        mockMvc.perform(put("/semester/" + saved.getPublicId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Cập nhật học kỳ thành công")));
    }

    @Test
    @DisplayName("DELETE /semester/{id} - Trả về 200 khi xóa học kỳ thành công")
    @WithMockUser(roles = "ADMIN")
    void delete_whenValid_shouldReturn200() throws Exception {
        Semester semester = Semester.builder()
                .name("Học kỳ Cần Xóa")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 5, 30))
                .status(SemesterStatus.UPCOMING)
                .build();
        Semester saved = semesterRepository.save(semester);

        mockMvc.perform(delete("/semester/" + saved.getPublicId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Xóa học kỳ thành công")));
    }
}
