package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.repository.TeacherRepository;
import com.example.tlu_gpms_system.features.account.service.AccountQueryService;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Chỉ được gọi nội bộ từ Service của feature khác, nên không đặt @PreAuthorize:
// method public của Service gọi tới đã kiểm tra quyền
@Service
@RequiredArgsConstructor
public class AccountQueryServiceImpl implements AccountQueryService {
    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Teacher> findTeacherByAccountId(Long accountId) {
        return teacherRepository.findAllByAccountIdIn(List.of(accountId)).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveStudentOrTeacherInFaculty(Faculty faculty) {
        return studentRepository.countActiveStudentsByFaculty(faculty) > 0
                || teacherRepository.countActiveTeachersByFaculty(faculty) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsStudentOrTeacherInFaculty(Faculty faculty) {
        return studentRepository.countByMajorFaculty(faculty) > 0
                || teacherRepository.countByMajorFaculty(faculty) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsStudentOrTeacherInMajor(Major major) {
        return studentRepository.countByMajor(major) > 0
                || teacherRepository.countByMajor(major) > 0;
    }
}
