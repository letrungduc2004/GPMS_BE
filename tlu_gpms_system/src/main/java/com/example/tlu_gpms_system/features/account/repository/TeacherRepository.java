package com.example.tlu_gpms_system.features.account.repository;

import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByPublicId(UUID publicId);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.major = :major")
    long countByMajor(@Param("major") Major major);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.major.faculty = :faculty")
    long countByMajorFaculty(@Param("faculty") Faculty faculty);

    @Query("SELECT COUNT(t) FROM Teacher t JOIN t.account a WHERE t.major.faculty = :faculty AND a.status = true")
    long countActiveTeachersByFaculty(@Param("faculty") Faculty faculty);

    @Query("SELECT COUNT(t) FROM Teacher t JOIN t.account a WHERE t.major = :major AND a.status = true")
    long countActiveTeachersByMajor(@Param("major") Major major);
}
