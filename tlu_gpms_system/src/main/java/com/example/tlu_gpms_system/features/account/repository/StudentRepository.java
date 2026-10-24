package com.example.tlu_gpms_system.features.account.repository;

import com.example.tlu_gpms_system.features.account.dto.response.StudentDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Điều kiện lọc dùng chung cho query danh sách và query đếm.
    // Tham số rỗng đã được Service đổi về null; null nghĩa là không lọc theo điều kiện đó.
    // keyword tìm theo mã SV, họ tên đầy đủ (Họ + Tên) hoặc email.
    // active: true = đang hoạt động (cả Account và Student đều bật), false = đã tắt.
    // allMajors = true (Admin): không lọc ngành; false: chỉ lấy sinh viên thuộc majorIds (phạm vi Trưởng ngành).
    String STUDENT_LIST_FILTER = """
            FROM Student s
            JOIN s.account a
            LEFT JOIN s.major m
            WHERE (:keyword IS NULL
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(CONCAT(COALESCE(a.firstName, ''), ' ', COALESCE(a.lastName, ''))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:batch IS NULL OR s.batch = :batch)
              AND (:majorId IS NULL OR m.publicId = :majorId)
              AND (:active IS NULL OR (CASE WHEN a.status = true AND s.status = true THEN true ELSE false END) = :active)
              AND (:allMajors = true OR m.id IN :majorIds)
            """;

    Optional<Student> findByPublicId(UUID publicId);

    boolean existsByStudentCode(String studentCode);

    // Dùng khi import: lấy 1 lần toàn bộ sinh viên đã tồn tại (tránh N+1)
    @Query("SELECT s FROM Student s JOIN FETCH s.account JOIN FETCH s.major WHERE s.studentCode IN :studentCodes")
    List<Student> findAllByStudentCodeIn(@Param("studentCodes") Collection<String> studentCodes);

    // Không có ORDER BY cố định: Service luôn truyền Sort đã qua whitelist (alias s., a., m. của query này)
    @Query(value = """
            SELECT new com.example.tlu_gpms_system.features.account.dto.response.StudentResponse(
                s.publicId,
                s.studentCode,
                TRIM(CONCAT(COALESCE(a.firstName, ''), ' ', COALESCE(a.lastName, ''))),
                m.name,
                s.className,
                CASE WHEN a.status = true AND s.status = true THEN true ELSE false END
            )
            """ + STUDENT_LIST_FILTER,
            countQuery = "SELECT COUNT(s) " + STUDENT_LIST_FILTER)
    Page<StudentResponse> findAllStudents(
            @Param("keyword") String keyword,
            @Param("batch") String batch,
            @Param("majorId") UUID majorId,
            @Param("active") Boolean active,
            @Param("allMajors") boolean allMajors,
            @Param("majorIds") Collection<Long> majorIds,
            Pageable pageable
    );

    @Query("""
            SELECT new com.example.tlu_gpms_system.features.account.dto.response.StudentDetailResponse(
                s.publicId,
                s.studentCode,
                TRIM(CONCAT(COALESCE(a.firstName, ''), ' ', COALESCE(a.lastName, ''))),
                a.email,
                a.phone,
                m.name,
                s.className,
                s.batch,
                CASE WHEN a.status = true AND s.status = true THEN true ELSE false END
            )
            FROM Student s
            JOIN s.account a
            LEFT JOIN s.major m
            WHERE s.publicId = :publicId
              AND (:allMajors = true OR m.id IN :majorIds)
            """)
    Optional<StudentDetailResponse> findStudentDetail(@Param("publicId") UUID publicId,
                                                      @Param("allMajors") boolean allMajors,
                                                      @Param("majorIds") Collection<Long> majorIds);


    //------------------------------------------Dương------------------------------------
    @Query("SELECT COUNT(s) FROM Student s WHERE s.major = :major")
    long countByMajor(@Param("major") com.example.tlu_gpms_system.features.faculty.entity.Major major);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.major.faculty = :faculty")
    long countByMajorFaculty(@Param("faculty") com.example.tlu_gpms_system.features.faculty.entity.Faculty faculty);

    @Query("SELECT COUNT(s) FROM Student s JOIN s.account a WHERE s.major.faculty = :faculty AND s.status = true AND a.status = true")
    long countActiveStudentsByFaculty(@Param("faculty") com.example.tlu_gpms_system.features.faculty.entity.Faculty faculty);

    @Query("SELECT COUNT(s) FROM Student s JOIN s.account a WHERE s.major = :major AND s.status = true AND a.status = true")
    long countActiveStudentsByMajor(@Param("major") com.example.tlu_gpms_system.features.faculty.entity.Major major);
}
