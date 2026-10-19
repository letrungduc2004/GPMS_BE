package com.example.tlu_gpms_system.features.account.repository;

import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByPublicId(UUID publicId);

    boolean existsByStudentCode(String studentCode);

    @Query(value = """
        SELECT new com.example.tlu_gpms_system.features.account.dto.response.StudentResponse(
            s.publicId,
            s.studentCode,
            TRIM(CONCAT(COALESCE(a.lastName, ''), ' ', COALESCE(a.firstName, ''))),
            m.name,
            s.className,
            s.batch
        )
        FROM Student s
        JOIN s.account a
        LEFT JOIN s.major m
        WHERE a.status = true
          AND (:lastName IS NULL OR :lastName = '' OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
          AND (:batch IS NULL OR :batch = '' OR s.batch = :batch)
          AND (:majorName IS NULL OR :majorName = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :majorName, '%')))
    """,
    countQuery = """
        SELECT COUNT(s)
        FROM Student s
        JOIN s.account a
        LEFT JOIN s.major m
        WHERE a.status = true
          AND (:lastName IS NULL OR :lastName = '' OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
          AND (:batch IS NULL OR :batch = '' OR s.batch = :batch)
          AND (:majorName IS NULL OR :majorName = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :majorName, '%')))
    """)
    Page<StudentResponse> findAllStudents(
        @Param("lastName") String lastName,
        @Param("batch") String batch,
        @Param("majorName") String majorName,
        Pageable pageable
    );

    @Query("SELECT COUNT(s) FROM Student s WHERE s.major = :major")
    long countByMajor(@Param("major") com.example.tlu_gpms_system.features.faculty.entity.Major major);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.major.faculty = :faculty")
    long countByMajorFaculty(@Param("faculty") com.example.tlu_gpms_system.features.faculty.entity.Faculty faculty);

    @Query("SELECT COUNT(s) FROM Student s JOIN s.account a WHERE s.major.faculty = :faculty AND s.status = true AND a.status = true")
    long countActiveStudentsByFaculty(@Param("faculty") com.example.tlu_gpms_system.features.faculty.entity.Faculty faculty);

    @Query("SELECT COUNT(s) FROM Student s JOIN s.account a WHERE s.major = :major AND s.status = true AND a.status = true")
    long countActiveStudentsByMajor(@Param("major") com.example.tlu_gpms_system.features.faculty.entity.Major major);
}
