package com.example.tlu_gpms_system.features.account.repository;

import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByPublicId(UUID publicId);

    @Query(value = """
        SELECT new com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse(
            t.publicId,
            TRIM(CONCAT(COALESCE(a.lastName, ''), ' ', COALESCE(a.firstName, ''))),
            m.name,
            t.organization,
            a.phone,
            t.amount,
            a.status
        )
        FROM Teacher t
        JOIN t.account a
        LEFT JOIN t.major m
        WHERE (:lastName IS NULL OR :lastName = '' OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
          AND (:organization IS NULL OR :organization = '' OR LOWER(t.organization) LIKE LOWER(CONCAT('%', :organization, '%')))
          AND (:majorName IS NULL OR :majorName = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :majorName, '%')))
        ORDER BY a.status DESC, t.id DESC
    """,
    countQuery = """
        SELECT COUNT(t)
        FROM Teacher t
        JOIN t.account a
        LEFT JOIN t.major m
        WHERE (:lastName IS NULL OR :lastName = '' OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
          AND (:organization IS NULL OR :organization = '' OR LOWER(t.organization) LIKE LOWER(CONCAT('%', :organization, '%')))
          AND (:majorName IS NULL OR :majorName = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :majorName, '%')))
    """)
    Page<TeacherResponse> findAllTeachers(
        @Param("lastName") String lastName,
        @Param("organization") String organization,
        @Param("majorName") String majorName,
        Pageable pageable
    );
}
