package com.example.tlu_gpms_system.features.account.repository;
import com.example.tlu_gpms_system.features.account.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
}
