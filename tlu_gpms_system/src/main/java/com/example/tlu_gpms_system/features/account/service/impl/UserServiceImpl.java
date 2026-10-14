package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.features.account.entity.Role;

import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Major;
import com.example.tlu_gpms_system.features.account.entity.Student;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.MajorRepository;
import com.example.tlu_gpms_system.features.account.repository.RoleRepository;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final StudentRepository studentRepository;
    private final AccountRepository accountRepository;
    private final MajorRepository majorRepository;
    private final RoleRepository roleRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public Page<StudentResponse> getAllStudents(String lastName, String batch, String majorName, Pageable pageable) {
        return studentRepository.findAllStudents(lastName, batch, majorName, pageable);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public void createStudents(List<StudentCreationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        Role studentRole = roleRepository.findById(com.example.tlu_gpms_system.common.enums.Role.STUDENT.name())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));

        for (StudentCreationRequest request : requests) {
            if (accountRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_EXIST);
            }
            if (studentRepository.existsByStudentCode(request.getStudentCode())) {
                throw new AppException(ErrorCode.STUDENT_CODE_EXIST);
            }

            Major major = majorRepository.findById(request.getMajorId())
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

            Account account = Account.builder()
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .status(true)
                    .roles(new HashSet<>(Set.of(studentRole)))
                    .build();

            Account savedAccount = accountRepository.save(account);

            Student student = Student.builder()
                    .studentCode(request.getStudentCode())
                    .batch(request.getBatch())
                    .className(request.getClassName())
                    .status(true)
                    .major(major)
                    .account(savedAccount)
                    .build();
            studentRepository.save(student);
        }
    }
}
