package com.example.tlu_gpms_system.features.account.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/student")
    public APIResponse<Page<StudentResponse>> getAllStudent(
            @RequestParam(name = "lastName", required = false) String lastName,
            @RequestParam(name = "batch", required = false) String batch,
            @RequestParam(name = "majorName", required = false) String majorName,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return APIResponse.<Page<StudentResponse>>builder()
                .code(1000)
                .data(userService.getAllStudents(lastName, batch, majorName, pageable))
                .build();
    }

    @PostMapping("/student")
    public APIResponse<Void> createStudent(@RequestBody @Valid List<StudentCreationRequest> requests) {
        userService.createStudents(requests);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Thêm sinh viên thành công")
                .build();
    }
//
//    @GetMapping("/teacher")
//    public APIResponse<Page<Object>> getAllTeacher() {
//        return APIResponse.builder().build();
//    }

    @PostMapping("/teacher")
    public APIResponse<Object> createTeacher() {
        return APIResponse.builder().build();
    }
}
