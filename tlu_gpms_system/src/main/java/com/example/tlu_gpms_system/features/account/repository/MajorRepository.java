package com.example.tlu_gpms_system.features.account.repository;

import com.example.tlu_gpms_system.features.account.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    Optional<Major> findByPublicId(UUID publicId);
}
