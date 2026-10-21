package com.example.tlu_gpms_system.features.account.repository;
import com.example.tlu_gpms_system.features.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByEmail(String userName);

    // Dùng khi import: lấy 1 lần toàn bộ tài khoản theo email (tránh N+1).
    // Không bọc LOWER(a.email) để còn dùng được index; collation mặc định của MySQL đã không phân biệt hoa/thường
    @Query("SELECT a FROM Account a WHERE a.email IN :emails")
    List<Account> findAllByEmailIn(@Param("emails") Collection<String> emails);

    // Dùng khi import: lấy 1 lần các tài khoản đang giữ số điện thoại trong file để chặn trùng SĐT giữa các tài khoản
    @Query("SELECT a FROM Account a WHERE a.phone IN :phones")
    List<Account> findAllByPhoneIn(@Param("phones") Collection<String> phones);

    @Query("select distinct u from Account u " +
            "left join fetch u.roles " +
            "where u.email =:email and u.status = true ")
    Optional<Account> findByEmailAndStatus(String email);

    @Query("select distinct u from Account u " +
            "left join fetch u.roles " +
            "where u.id =:accountId and u.status = true ")
    Optional<Account> findByIdAndStatus(Long accountId);
}
