package com.example.tlu_gpms_system.features.faculty.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.common.dto.PageResponse;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyDetailResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.HeadCandidateResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.service.FacultyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyService facultyService;

    @GetMapping
    public APIResponse<PageResponse<FacultyResponse>> getAll(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "DESC") String sortDir
    ) {
        String validSortBy = switch (sortBy) {
            case "code" -> "code";
            case "name" -> "name";
            case "headOfDepartment" -> "headOfDepartment.lastName";
            case "status" -> "status";
            case "id" -> "id";
            default -> "createdAt";
        };
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));

        return APIResponse.<PageResponse<FacultyResponse>>builder()
                .code(1000)
                .data(facultyService.getAll(keyword, status, pageable))
                .build();
    }

    @GetMapping("/head-candidates")
    public APIResponse<List<HeadCandidateResponse>> getHeadCandidates() {
        return APIResponse.<List<HeadCandidateResponse>>builder()
                .code(1000)
                .data(facultyService.getHeadCandidates())
                .build();
    }

    @GetMapping("/{id}")
    public APIResponse<FacultyDetailResponse> getById(@PathVariable("id") String id) {
        return APIResponse.<FacultyDetailResponse>builder()
                .code(1000)
                .data(facultyService.getById(id))
                .build();
    }

    @PostMapping
    public APIResponse<Void> create(@RequestBody @Valid FacultyCreationRequest request) {
        facultyService.create(request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Thêm mới khoa thành công")
                .build();
    }

    @PutMapping("/{id}")
    public APIResponse<Void> update(
            @PathVariable("id") String id,
            @RequestBody @Valid FacultyUpdateRequest request
    ) {
        facultyService.update(id, request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Cập nhật thông tin khoa thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable("id") String id) {
        facultyService.delete(id);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Xóa thành công")
                .build();
    }

    @GetMapping("/{id}/majors")
    public APIResponse<PageResponse<MajorResponse>> getMajors(
            @PathVariable("id") String id,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "DESC") String sortDir
    ) {
        String validSortBy = switch (sortBy) {
            case "name" -> "name";
            case "status" -> "status";
            case "id" -> "id";
            default -> "createdAt";
        };
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));

        return APIResponse.<PageResponse<MajorResponse>>builder()
                .code(1000)
                .data(facultyService.getMajorsByFaculty(id, keyword, status, pageable))
                .build();
    }

    @PostMapping("/{id}/majors")
    public APIResponse<Void> addMajor(
            @PathVariable("id") String id,
            @RequestBody @Valid MajorCreationRequest request
    ) {
        facultyService.addMajor(id, request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Thêm mới ngành thành công")
                .build();
    }
}
