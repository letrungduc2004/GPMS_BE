package com.example.tlu_gpms_system.features.account.repository;

import com.example.tlu_gpms_system.features.account.dto.response.TeacherDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.account.service.helper.TeacherOrganization;
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
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    // Điều kiện lọc dùng chung cho query danh sách và query đếm.
    // Tham số rỗng đã được Service đổi về null; null nghĩa là không lọc theo điều kiện đó.
    // name tìm theo họ tên đầy đủ (Họ + Tên), nên gõ "Nguyễn", "An" hay "Nguyễn Văn An" đều ra.
    // internal: true = trong trường (đơn vị đúng tên Thủy lợi), false = ngoài trường (trường khác)
    String TEACHER_LIST_FILTER = """
        FROM Teacher t
        JOIN t.account a
        LEFT JOIN t.major m
        WHERE (:name IS NULL
               OR LOWER(CONCAT(COALESCE(a.firstName, ''), ' ', COALESCE(a.lastName, ''))) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:majorName IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :majorName, '%')))
        """
        + "  AND (:internal IS NULL OR (CASE WHEN t.organization = '" + TeacherOrganization.TLU_NAME
        + "' THEN true ELSE false END) = :internal)\n";

    Optional<Teacher> findByPublicId(UUID publicId);

    // Dùng khi import: lấy 1 lần toàn bộ hồ sơ giáo viên theo account (tránh N+1)
    @Query("SELECT t FROM Teacher t JOIN FETCH t.account LEFT JOIN FETCH t.major WHERE t.account.id IN :accountIds")
    List<Teacher> findAllByAccountIdIn(@Param("accountIds") Collection<Long> accountIds);

    // Không có ORDER BY cố định: Service luôn truyền Sort đã qua whitelist (alias t., a., m. của query này)
    @Query(value = """
        SELECT new com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse(
            t.publicId,
            TRIM(CONCAT(COALESCE(a.firstName, ''), ' ', COALESCE(a.lastName, ''))),
            m.name,
            t.organization,
            a.phone,
            t.amount,
            a.status
        )
        """ + TEACHER_LIST_FILTER,
        countQuery = "SELECT COUNT(t) " + TEACHER_LIST_FILTER)
    Page<TeacherResponse> findAllTeachers(
        @Param("name") String name,
        @Param("internal") Boolean internal,
        @Param("majorName") String majorName,
        Pageable pageable
    );

    @Query("""
        SELECT new com.example.tlu_gpms_system.features.account.dto.response.TeacherDetailResponse(
            t.publicId,
            TRIM(CONCAT(COALESCE(a.firstName, ''), ' ', COALESCE(a.lastName, ''))),
            a.email,
            a.phone,
            t.organization,
            m.name,
            t.amount,
            a.status
        )
        FROM Teacher t
        JOIN t.account a
        LEFT JOIN t.major m
        WHERE t.publicId = :publicId
        """)
    Optional<TeacherDetailResponse> findTeacherDetail(@Param("publicId") UUID publicId);
}
