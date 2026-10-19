package com.example.tlu_gpms_system.features.faculty.controller;

import com.example.tlu_gpms_system.common.dto.PageResponse;
import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import com.example.tlu_gpms_system.configuration.exception.GlobalException;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyDetailResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.HeadCandidateResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.HeadOfDepartmentResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.service.FacultyService;
import com.example.tlu_gpms_system.features.faculty.service.MajorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FacultyControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FacultyService facultyService;

    @Mock
    private MajorService majorService;

    @InjectMocks
    private FacultyController facultyController;

    @InjectMocks
    private MajorController majorController;

    private UUID publicId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(facultyController, majorController)
                .setControllerAdvice(new GlobalException())
                .build();
        publicId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/faculties - Trả về danh sách khoa")
    void getAll_shouldReturnPageResponse() throws Exception {
        FacultyResponse item = FacultyResponse.builder()
                .id(publicId)
                .code("CNTT")
                .name("Công nghệ thông tin")
                .headOfDepartment(HeadOfDepartmentResponse.builder()
                        .id(10L)
                        .fullName("Nguyễn Văn A")
                        .build())
                .totalMajors(4L)
                .status("Hoạt động")
                .statusCode(ActiveStatus.ACTIVE)
                .build();

        PageResponse<FacultyResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1)
        );

        when(facultyService.getAll(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/faculties")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].code", is("CNTT")))
                .andExpect(jsonPath("$.data.content[0].totalMajors", is(4)))
                .andExpect(jsonPath("$.data.content[0].headOfDepartment.fullName", is("Nguyễn Văn A")));
    }

    @Test
    @DisplayName("GET /api/v1/faculties/head-candidates - Trả về danh sách ứng viên Trưởng khoa")
    void getHeadCandidates_shouldReturnList() throws Exception {
        HeadCandidateResponse candidate = HeadCandidateResponse.builder()
                .id(10L)
                .fullName("Nguyễn Văn A")
                .email("head@tlu.edu.vn")
                .build();

        when(facultyService.getHeadCandidates()).thenReturn(List.of(candidate));

        mockMvc.perform(get("/api/v1/faculties/head-candidates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.data[0].id", is(10)))
                .andExpect(jsonPath("$.data[0].fullName", is("Nguyễn Văn A")))
                .andExpect(jsonPath("$.data[0].email", is("head@tlu.edu.vn")));
    }

    @Test
    @DisplayName("GET /api/v1/faculties/{id} - Trả về chi tiết khoa")
    void getById_shouldReturnDetail() throws Exception {
        FacultyDetailResponse detail = FacultyDetailResponse.builder()
                .id(publicId)
                .code("CNTT")
                .name("Công nghệ thông tin")
                .headOfDepartment(HeadOfDepartmentResponse.builder()
                        .id(10L)
                        .fullName("Nguyễn Văn A")
                        .build())
                .description("Khoa đào tạo hàng đầu")
                .status("Hoạt động")
                .statusCode(ActiveStatus.ACTIVE)
                .build();

        when(facultyService.getById(publicId.toString())).thenReturn(detail);

        mockMvc.perform(get("/api/v1/faculties/{id}", publicId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.data.code", is("CNTT")))
                .andExpect(jsonPath("$.data.headOfDepartment.fullName", is("Nguyễn Văn A")));
    }

    @Test
    @DisplayName("POST /api/v1/faculties - Thêm mới khoa thành công")
    void create_shouldReturnSuccessMessage() throws Exception {
        FacultyCreationRequest request = FacultyCreationRequest.builder()
                .code("CNTT")
                .name("Công nghệ thông tin")
                .headOfDepartmentId(10L)
                .description("Khoa CNTT")
                .status(ActiveStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/faculties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Thêm mới khoa thành công")));

        verify(facultyService).create(any(FacultyCreationRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/faculties/{id} - Cập nhật thông tin khoa thành công")
    void update_shouldReturnSuccessMessage() throws Exception {
        FacultyUpdateRequest request = FacultyUpdateRequest.builder()
                .code("CNTT")
                .name("Công nghệ thông tin")
                .headOfDepartmentId(10L)
                .status(ActiveStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/faculties/{id}", publicId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Cập nhật thông tin khoa thành công")));

        verify(facultyService).update(eq(publicId.toString()), any(FacultyUpdateRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/faculties/{id} - Xóa khoa thành công")
    void delete_shouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/v1/faculties/{id}", publicId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Xóa thành công")));

        verify(facultyService).delete(publicId.toString());
    }

    @Test
    @DisplayName("GET /api/v1/faculties/{id}/majors - Trả về danh sách ngành theo khoa")
    void getMajors_shouldReturnMajors() throws Exception {
        MajorResponse major = MajorResponse.builder()
                .id(UUID.randomUUID())
                .name("Công nghệ thông tin")
                .description("Kiến thức nền tảng về lập trình...")
                .status("Hoạt động")
                .statusCode(ActiveStatus.ACTIVE)
                .build();

        PageResponse<MajorResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(major), PageRequest.of(0, 10), 1)
        );

        when(facultyService.getMajorsByFaculty(eq(publicId.toString()), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/faculties/{id}/majors", publicId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.data.content[0].name", is("Công nghệ thông tin")))
                .andExpect(jsonPath("$.data.content[0].status", is("Hoạt động")));
    }

    @Test
    @DisplayName("POST /api/v1/faculties/{id}/majors - Thêm mới ngành vào khoa thành công")
    void addMajor_shouldReturnSuccessMessage() throws Exception {
        MajorCreationRequest request = MajorCreationRequest.builder()
                .name("Khoa học máy tính")
                .description("Nghiên cứu cấu trúc dữ liệu và giải thuật")
                .status(ActiveStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/faculties/{id}/majors", publicId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Thêm mới ngành thành công")));

        verify(facultyService).addMajor(eq(publicId.toString()), any(MajorCreationRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/majors/{majorId} - Lấy chi tiết ngành thành công")
    void getMajorById_shouldReturnMajor() throws Exception {
        UUID majorId = UUID.randomUUID();
        MajorResponse major = MajorResponse.builder()
                .id(majorId)
                .name("Khoa học dữ liệu")
                .status("Hoạt động")
                .build();

        when(majorService.getById(majorId.toString())).thenReturn(major);

        mockMvc.perform(get("/api/v1/majors/{majorId}", majorId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.data.name", is("Khoa học dữ liệu")));
    }

    @Test
    @DisplayName("PUT /api/v1/majors/{majorId} - Cập nhật ngành thành công")
    void updateMajor_shouldReturnSuccessMessage() throws Exception {
        UUID majorId = UUID.randomUUID();
        MajorUpdateRequest request = MajorUpdateRequest.builder()
                .name("Khoa học dữ liệu")
                .status(ActiveStatus.ACTIVE)
                .description("Mô tả ngành KHDL")
                .build();

        mockMvc.perform(put("/api/v1/majors/{majorId}", majorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Cập nhật ngành thành công")));

        verify(majorService).update(eq(majorId.toString()), any(MajorUpdateRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/majors/{majorId} - Xóa ngành thành công")
    void deleteMajor_shouldReturnSuccessMessage() throws Exception {
        UUID majorId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/majors/{majorId}", majorId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1000)))
                .andExpect(jsonPath("$.message", is("Xóa thành công")));

        verify(majorService).delete(majorId.toString());
    }
}
