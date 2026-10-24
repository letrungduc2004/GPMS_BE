package com.example.tlu_gpms_system.features.faculty.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorHeadRequest;
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

    @GetMapping({"/api/v1/majors", "/api/v1/major", "/majors", "/major"})
    public APIResponse<List<MajorResponse>> getAllMajors() {
        return APIResponse.<List<MajorResponse>>builder()
                .code(1000)
                .data(majorService.getAllMajors())
                .build();
    }

    @GetMapping({"/api/v1/majors/{majorId}", "/api/v1/major/{majorId}", "/majors/{majorId}", "/major/{majorId}"})
    public APIResponse<MajorResponse> getById(@PathVariable("majorId") String majorId) {
        return APIResponse.<MajorResponse>builder()
                .code(1000)
                .data(majorService.getById(majorId))
                .build();
    }

    @PutMapping({"/api/v1/majors/{majorId}", "/api/v1/major/{majorId}", "/majors/{majorId}", "/major/{majorId}"})
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

    @PutMapping({"/api/v1/majors/{majorId}/head", "/api/v1/major/{majorId}/head", "/majors/{majorId}/head", "/major/{majorId}/head"})
    public APIResponse<Void> assignHead(
            @PathVariable("majorId") String majorId,
            @RequestBody @Valid MajorHeadRequest request
    ) {
        majorService.assignHead(majorId, request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Gán Trưởng ngành thành công")
                .build();
    }

    @DeleteMapping({"/api/v1/majors/{majorId}", "/api/v1/major/{majorId}", "/majors/{majorId}", "/major/{majorId}"})
    public APIResponse<Void> delete(@PathVariable("majorId") String majorId) {
        majorService.delete(majorId);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Xóa thành công")
                .build();
    }
}
