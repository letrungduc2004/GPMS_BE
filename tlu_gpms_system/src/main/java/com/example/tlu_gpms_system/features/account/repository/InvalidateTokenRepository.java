package com.example.tlu_gpms_system.features.account.repository;
import com.example.tlu_gpms_system.features.account.entity.InvalidateToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidateTokenRepository extends JpaRepository<InvalidateToken, String> {

    @Modifying
    @Query("delete from InvalidateToken t where t.expirationTime < CURRENT_TIMESTAMP")
    void cleanUpToken();
}
