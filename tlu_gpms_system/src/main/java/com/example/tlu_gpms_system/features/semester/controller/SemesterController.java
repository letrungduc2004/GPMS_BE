package com.example.tlu_gpms_system.features.semester.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.common.dto.PageResponse;
import com.example.tlu_gpms_system.features.semester.dto.request.SemesterRequest;
import com.example.tlu_gpms_system.features.semester.dto.response.SemesterResponse;
import com.example.tlu_gpms_system.features.semester.service.SemesterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping
    public APIResponse<PageResponse<SemesterResponse>> getAll(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "startDate") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "DESC") String sortDir
    ) {
        String validSortBy = switch (sortBy) {
            case "name" -> "name";
            case "endDate" -> "endDate";
            case "status" -> "status";
            case "createdAt" -> "createdAt";
            case "id" -> "id";
            default -> "startDate";
        };
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));

        return APIResponse.<PageResponse<SemesterResponse>>builder()
                .code(1000)
                .data(semesterService.getAll(keyword, status, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public APIResponse<SemesterResponse> getById(@PathVariable("id") UUID id) {
        return APIResponse.<SemesterResponse>builder()
                .code(1000)
                .data(semesterService.getByPublicId(id))
                .build();
    }

    @PostMapping
    public APIResponse<Void> create(@RequestBody @Valid SemesterRequest request) {
        semesterService.create(request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Thêm mới học kỳ thành công")
                .build();
    }

    @PutMapping("/{id}")
    public APIResponse<Void> update(
            @PathVariable("id") UUID id,
            @RequestBody @Valid SemesterRequest request
    ) {
        semesterService.update(id, request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Cập nhật học kỳ thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable("id") UUID id) {
        semesterService.delete(id);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Xóa học kỳ thành công")
                .build();
    }
}
