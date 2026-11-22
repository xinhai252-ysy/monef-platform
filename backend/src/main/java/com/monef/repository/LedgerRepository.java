package com.monef.repository;

import com.monef.entity.LedgerEntry;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LedgerRepository
    extends JpaRepository<LedgerEntry, String>, JpaSpecificationExecutor<LedgerEntry>
{
    Optional<LedgerEntry> findByTenantIdAndClientRef(String tenant, String clientRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LedgerEntry l where l.id=:id and l.tenantId=:tenant")
    Optional<LedgerEntry> lock(@Param("id") String id, @Param("tenant") String tenant);

    @Query(
        "select coalesce(sum(case when l.kind='INCOME' then l.amount else -l.amount end),0) from LedgerEntry l where l.tenantId=:tenant and l.accountId=:account and l.status='POSTED'"
    )
    BigDecimal balance(@Param("tenant") String tenant, @Param("account") String account);

    @Query(
        "select l.kind, sum(l.baseAmount), count(l) from LedgerEntry l where l.tenantId=:tenant and l.status='POSTED' and l.bookedOn between :start and :end group by l.kind"
    )
    List<Object[]> totals(
        @Param("tenant") String tenant,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query(
        "select year(l.bookedOn),month(l.bookedOn),l.kind,sum(l.baseAmount) from LedgerEntry l where l.tenantId=:tenant and l.status='POSTED' and l.bookedOn between :start and :end group by year(l.bookedOn),month(l.bookedOn),l.kind order by year(l.bookedOn),month(l.bookedOn)"
    )
    List<Object[]> monthly(
        @Param("tenant") String tenant,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query(
        "select l.category, sum(l.baseAmount), count(l) from LedgerEntry l where l.tenantId=:tenant and l.status='POSTED' and l.kind='EXPENSE' and l.bookedOn between :start and :end group by l.category order by sum(l.baseAmount) desc"
    )
    List<Object[]> categories(
        @Param("tenant") String tenant,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );
}
