package com.example.tlu_gpms_system.features.semester.repository;

import com.example.tlu_gpms_system.common.enums.SemesterStatus;
import com.example.tlu_gpms_system.features.semester.entity.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findByPublicId(UUID publicId);

    boolean existsByName(String name);

    boolean existsByNameAndPublicIdNot(String name, UUID publicId);

    @Query(value = """
        SELECT s FROM Semester s
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR s.status = :status)
    """,
    countQuery = """
        SELECT COUNT(s) FROM Semester s
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR s.status = :status)
    """)
    Page<Semester> findAllWithFilter(
        @Param("keyword") String keyword,
        @Param("status") SemesterStatus status,
        Pageable pageable
    );
}
