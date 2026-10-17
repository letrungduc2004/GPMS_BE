package com.example.tlu_gpms_system.features.account.repository;
import com.example.tlu_gpms_system.features.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByEmail(String userName);

    @Query("select distinct u from Account u " +
            "left join fetch u.roles " +
            "where u.email =:email and u.status = true ")
    Optional<Account> findByEmailAndStatus(String email);

    @Query("select distinct u from Account u " +
            "left join fetch u.roles " +
            "where u.id =:accountId and u.status = true ")
    Optional<Account> findByIdAndStatus(Long accountId);
}
