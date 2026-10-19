package com.example.tlu_gpms_system.features.account.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.features.account.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.account.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    // Dropdown "Ngành" trên màn danh sách sinh viên: FE lấy publicId gửi lại làm majorId
    @GetMapping("/major")
    public APIResponse<List<MajorResponse>> getAllMajors() {
        return APIResponse.<List<MajorResponse>>builder()
                .code(1000)
                .data(majorService.getAllMajors())
                .build();
    }
}
