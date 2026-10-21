package com.example.tlu_gpms_system.features.account.service;

import com.example.tlu_gpms_system.common.enums.AccountStatusFilter;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherAmountRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {
    // Danh sách sinh viên: keyword tìm theo mã SV / họ tên / email, majorId là publicId ngành lấy từ GET /major.
    // Chỉ sort được theo các trường trong whitelist, trường khác ném INVALID_SORT_FIELD
    Page<StudentResponse> getAllStudents(String keyword, String batch, UUID majorId, AccountStatusFilter status, Pageable pageable);
    // Popup chi tiết 1 sinh viên. publicId sai định dạng hoặc không có -> STUDENT_NOT_FOUND
    StudentDetailResponse getStudentDetail(String publicId);
    // Danh sách giáo viên: name tìm theo họ tên đầy đủ, majorName tìm gần đúng,
    // organizationType = "internal" (trong trường) / "external" (ngoài trường), trống thì không lọc.
    // Chỉ sort được theo các trường trong whitelist, trường khác ném INVALID_SORT_FIELD
    Page<TeacherResponse> getAllTeachers(String name, String organizationType, String majorName, Pageable pageable);
    // Popup chi tiết 1 giáo viên. publicId sai định dạng hoặc không có -> TEACHER_NOT_FOUND
    TeacherDetailResponse getTeacherDetail(String publicId);

    // Cách 1: import danh sách sinh viên từ file Excel (lưu một phần: dòng hợp lệ vẫn được lưu)
    StudentImportResponse importStudents(MultipartFile file);
    // Cách 2: nhập thủ công 1 sinh viên. Trả về code + message cho FE (xanh/vàng), lỗi thì ném AppException
    StudentCreationResult createStudent(StudentCreationRequest request);
    // Cách 3: kích hoạt lại sinh viên đã tắt ngay trên danh sách. Trả về false nếu sinh viên đang hoạt động sẵn
    boolean activateStudent(String publicId);

    // Cách 1: import danh sách giáo viên từ file Excel (lưu một phần: dòng hợp lệ vẫn được lưu)
    TeacherImportResponse importTeachers(MultipartFile file);
    // Cách 2: nhập thủ công 1 giáo viên. Trả về code + message (thêm mới / kích hoạt lại / cảnh báo), lỗi thì ném AppException
    TeacherCreationResult createTeacher(TeacherCreationRequest request);
    // Cách 3: kích hoạt lại giáo viên đã tắt ngay trên danh sách. Trả về false nếu giáo viên đang hoạt động sẵn
    boolean activateTeacher(String publicId);
    // Sửa số lượng sinh viên hướng dẫn của giáo viên đang hoạt động. Lỗi thì ném AppException
    void updateTeacherAmount(String publicId, TeacherAmountRequest request);
}
