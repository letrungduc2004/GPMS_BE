package com.example.tlu_gpms_system.features.account.repository;
import com.example.tlu_gpms_system.features.account.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
}
