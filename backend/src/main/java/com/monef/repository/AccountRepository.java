package com.monef.repository;

import com.monef.entity.Account;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByTenantIdOrderByCreatedAt(String tenantId);
    Optional<Account> findByIdAndTenantId(String id, String tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id=:id and a.tenantId=:tenant")
    Optional<Account> lock(@Param("id") String id, @Param("tenant") String tenant);
}
