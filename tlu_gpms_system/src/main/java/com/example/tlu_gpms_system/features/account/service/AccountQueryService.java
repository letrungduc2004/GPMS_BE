package com.example.tlu_gpms_system.features.account.service;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import com.example.tlu_gpms_system.features.faculty.entity.Major;

import java.util.Optional;

// Service chỉ đọc, cung cấp dữ liệu tài khoản / sinh viên / giáo viên cho feature khác (faculty...)
// Tách riêng khỏi UserService để tránh vòng phụ thuộc: UserServiceImpl đã gọi MajorService
public interface AccountQueryService {

    Optional<Account> findAccountById(Long accountId);

    // Hồ sơ giáo viên (kèm account và major) của tài khoản; dùng khi gán Trưởng ngành
    Optional<Teacher> findTeacherByAccountId(Long accountId);

    // Khoa còn sinh viên hoặc giáo viên đang hoạt động
    boolean existsActiveStudentOrTeacherInFaculty(Faculty faculty);

    // Khoa còn bất kỳ sinh viên hoặc giáo viên nào (kể cả không hoạt động)
    boolean existsStudentOrTeacherInFaculty(Faculty faculty);

    // Ngành còn bất kỳ sinh viên hoặc giáo viên nào (kể cả không hoạt động)
    boolean existsStudentOrTeacherInMajor(Major major);
}
