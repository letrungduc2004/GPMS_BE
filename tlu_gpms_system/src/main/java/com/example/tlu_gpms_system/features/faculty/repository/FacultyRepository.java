package com.example.tlu_gpms_system.features.faculty.repository;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByPublicId(UUID publicId);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query(value = "SELECT f FROM Faculty f " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(f.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR f.status = :status)",
            countQuery = "SELECT COUNT(f) FROM Faculty f " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(f.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR f.status = :status)")
    Page<Faculty> findAllWithFilter(
            @Param("keyword") String keyword,
            @Param("status") ActiveStatus status,
            Pageable pageable
    );
}
