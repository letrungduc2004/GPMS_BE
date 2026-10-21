package com.example.tlu_gpms_system.features.faculty.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.service.MajorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    @GetMapping("/major")
    public APIResponse<List<com.example.tlu_gpms_system.features.account.dto.response.MajorResponse>> getAllMajors() {
        return APIResponse.<List<com.example.tlu_gpms_system.features.account.dto.response.MajorResponse>>builder()
                .code(1000)
                .data(majorService.getAllMajors())
                .build();
    }

    @GetMapping("/{majorId}")
    public APIResponse<MajorResponse> getById(@PathVariable("majorId") String majorId) {
        return APIResponse.<MajorResponse>builder()
                .code(1000)
                .data(majorService.getById(majorId))
                .build();
    }

    @PutMapping("/{majorId}")
    public APIResponse<Void> update(
            @PathVariable("majorId") String majorId,
            @RequestBody @Valid MajorUpdateRequest request
    ) {
        majorService.update(majorId, request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Cập nhật ngành thành công")
                .build();
    }

    @DeleteMapping("/{majorId}")
    public APIResponse<Void> delete(@PathVariable("majorId") String majorId) {
        majorService.delete(majorId);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Xóa thành công")
                .build();
    }
}
