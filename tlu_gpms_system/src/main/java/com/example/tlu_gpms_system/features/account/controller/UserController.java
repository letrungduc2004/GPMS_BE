package com.example.tlu_gpms_system.features.account.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.common.enums.AccountStatusFilter;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import com.example.tlu_gpms_system.features.account.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherAmountRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Danh sách sinh viên. Ví dụ: /student?keyword=đức&majorId=<publicId ngành>&status=ACTIVE&sort=name,asc
    // status: ACTIVE (mặc định) | INACTIVE | ALL. sort: studentCode | name | majorName | className
    @GetMapping("/student")
    public APIResponse<Page<StudentResponse>> getAllStudent(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "batch", required = false) String batch,
            @RequestParam(name = "majorId", required = false) UUID majorId,
            @RequestParam(name = "status", defaultValue = "ACTIVE") AccountStatusFilter status,
            @PageableDefault(size = 15) Pageable pageable) {
        return APIResponse.<Page<StudentResponse>>builder()
                .code(1000)
                .data(userService.getAllStudents(keyword, batch, majorId, status, pageable))
                .build();
    }

    // Popup chi tiết khi bấm vào 1 dòng trên danh sách sinh viên
    @GetMapping("/student/{publicId}")
    public APIResponse<StudentDetailResponse> getStudentDetail(@PathVariable(name = "publicId") String publicId) {
        return APIResponse.<StudentDetailResponse>builder()
                .code(1000)
                .data(userService.getStudentDetail(publicId))
                .build();
    }

    // Cách 1: import Excel. Luôn trả 200 kèm kết quả từng dòng (lỗi mức toàn file mới trả 400)
    @PostMapping(value = "/student/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public APIResponse<StudentImportResponse> importStudents(@RequestPart("file") MultipartFile file) {
        return APIResponse.<StudentImportResponse>builder()
                .code(1000)
                .message("Xử lý file import hoàn tất")
                .data(userService.importStudents(file))
                .build();
    }

    // Cách 2: nhập thủ công 1 sinh viên. Message tùy kết quả (thêm mới / kích hoạt lại / cập nhật field nào),
    // nhập lại y hệt sinh viên đang hoạt động trả 200 với code 1101 để FE báo vàng
    @PostMapping("/student")
    public APIResponse<Void> createStudent(@RequestBody StudentCreationRequest request) {
        StudentCreationResult result = userService.createStudent(request);
        return APIResponse.<Void>builder()
                .code(result.getCode())
                .message(result.getMessage())
                .build();
    }

    // Cách 3: bấm nút Kích hoạt tài khoản trên danh sách. Sinh viên đang hoạt động sẵn trả 200 với code 1100 để FE báo vàng
    @PatchMapping("/student/{publicId}/activate")
    public APIResponse<Void> activateStudent(@PathVariable(name = "publicId") String publicId) {
        if (!userService.activateStudent(publicId)) {
            return APIResponse.<Void>builder()
                    .code(ErrorCode.STUDENT_ALREADY_ACTIVE.getCode())
                    .message(ErrorCode.STUDENT_ALREADY_ACTIVE.getMessage())
                    .build();
        }
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Đã kích hoạt lại tài khoản sinh viên")
                .build();
    }


//------------------------------------------------------TEACHER -----------------------------------------------


    // Không đặt sort mặc định ở đây: không gửi sort thì Service tự sắp đang hoạt động trước, mới thêm trước
    @GetMapping("/teacher")
    public APIResponse<Page<TeacherResponse>> getAllTeacher(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "organizationType", required = false) String organizationType,
            @RequestParam(name = "majorName", required = false) String majorName,
            @PageableDefault(size = 10) Pageable pageable) {
        return APIResponse.<Page<TeacherResponse>>builder()
                .code(1000)
                .data(userService.getAllTeachers(name, organizationType, majorName, pageable))
                .build();
    }

    // Popup chi tiết khi bấm vào 1 dòng trên danh sách giáo viên
    @GetMapping("/teacher/{publicId}")
    public APIResponse<TeacherDetailResponse> getTeacherDetail(@PathVariable(name = "publicId") String publicId) {
        return APIResponse.<TeacherDetailResponse>builder()
                .code(1000)
                .data(userService.getTeacherDetail(publicId))
                .build();
    }

    // Cách 1: import Excel. Luôn trả 200 kèm kết quả từng dòng (lỗi mức toàn file mới trả 400)
    @PostMapping(value = "/teacher/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public APIResponse<TeacherImportResponse> importTeachers(@RequestPart("file") MultipartFile file) {
        return APIResponse.<TeacherImportResponse>builder()
                .code(1000)
                .message("Xử lý file import hoàn tất")
                .data(userService.importTeachers(file))
                .build();
    }

    // Cách 2: nhập thủ công 1 giáo viên. Message tùy kết quả (thêm mới / kích hoạt lại / cập nhật field nào),
    // TH4 (đang hoạt động) trả 200 với code 1100 để FE báo vàng
    @PostMapping("/teacher")
    public APIResponse<Void> createTeacher(@RequestBody TeacherCreationRequest request) {
        TeacherCreationResult result = userService.createTeacher(request);
        return APIResponse.<Void>builder()
                .code(result.getCode())
                .message(result.getMessage())
                .build();
    }

    // Cách 3: bấm nút Kích hoạt tài khoản trên danh sách. Giáo viên đang hoạt động sẵn trả 200 với code 1100 để FE báo vàng
    @PatchMapping("/teacher/{publicId}/activate")
    public APIResponse<Void> activateTeacher(@PathVariable(name = "publicId") String publicId) {
        if (!userService.activateTeacher(publicId)) {
            return APIResponse.<Void>builder()
                    .code(ErrorCode.TEACHER_ALREADY_ACTIVE.getCode())
                    .message(ErrorCode.TEACHER_ALREADY_ACTIVE.getMessage())
                    .build();
        }
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Đã kích hoạt lại tài khoản giáo viên")
                .build();
    }

    // Sửa số lượng sinh viên hướng dẫn của giáo viên đang hoạt động. Body: {"amount": 15}
    @PatchMapping("/teacher/{publicId}/amount")
    public APIResponse<Void> updateTeacherAmount(@PathVariable(name = "publicId") String publicId,
                                                 @RequestBody TeacherAmountRequest request) {
        userService.updateTeacherAmount(publicId, request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Cập nhật số lượng hướng dẫn thành công")
                .build();
    }
}
