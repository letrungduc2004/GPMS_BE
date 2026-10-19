package com.example.tlu_gpms_system.features.account.repository;

import com.example.tlu_gpms_system.features.account.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.account.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    Optional<Major> findByPublicId(UUID publicId);

    // Dữ liệu cho dropdown "Ngành"
    @Query("SELECT new com.example.tlu_gpms_system.features.account.dto.response.MajorResponse(m.publicId, m.code, m.name) FROM Major m ORDER BY m.name")
    List<MajorResponse> findAllMajors();
}
