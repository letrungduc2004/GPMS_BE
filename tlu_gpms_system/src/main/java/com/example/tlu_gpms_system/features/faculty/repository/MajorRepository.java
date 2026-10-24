package com.example.tlu_gpms_system.features.faculty.repository;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {

    // Dữ liệu cho dropdown "Ngành"
    @Query("SELECT new com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse(m.publicId, m.code, m.name) FROM Major m ORDER BY m.name")
    List<MajorResponse> findAllMajors();

    Optional<Major> findByPublicId(UUID publicId);

    boolean existsByNameAndFaculty(String name, Faculty faculty);

    boolean existsByNameAndFacultyAndIdNot(String name, Faculty faculty, Long id);

    // Tài khoản đã là Trưởng ngành của ngành khác chưa (mỗi người làm trưởng tối đa 1 ngành)
    boolean existsByHeadIdAndIdNot(Long headId, Long id);

    // id các ngành mà tài khoản đang làm Trưởng ngành (dùng tính phạm vi ngành mỗi request)
    @Query("SELECT m.id FROM Major m WHERE m.head.id = :accountId")
    List<Long> findIdsByHeadId(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(m) FROM Major m WHERE m.faculty = :faculty")
    long countByFaculty(@Param("faculty") Faculty faculty);

    @Query("SELECT m.faculty.id, COUNT(m) FROM Major m WHERE m.faculty.id IN :facultyIds GROUP BY m.faculty.id")
    List<Object[]> countMajorsByFacultyIds(@Param("facultyIds") List<Long> facultyIds);

    @Query("SELECT COUNT(m) > 0 FROM Major m WHERE m.faculty = :faculty AND m.status = :status")
    boolean existsByFacultyAndStatus(@Param("faculty") Faculty faculty, @Param("status") ActiveStatus status);

    @Query(value = "SELECT m FROM Major m WHERE m.faculty = :faculty " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR m.status = :status)",
            countQuery = "SELECT COUNT(m) FROM Major m WHERE m.faculty = :faculty " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR m.status = :status)")
    Page<Major> findMajorsByFacultyWithFilter(
            @Param("faculty") Faculty faculty,
            @Param("keyword") String keyword,
            @Param("status") ActiveStatus status,
            Pageable pageable
    );
}
