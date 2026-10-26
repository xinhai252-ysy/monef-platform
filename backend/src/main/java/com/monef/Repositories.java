package com.monef;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

interface UserRepository extends JpaRepository<AppUser,String> { Optional<AppUser> findByEmail(String email); }
interface AccountRepository extends JpaRepository<Account,String> {
 List<Account> findByTenantIdOrderByCreatedAt(String tenantId);
 Optional<Account> findByIdAndTenantId(String id,String tenantId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select a from Account a where a.id=:id and a.tenantId=:tenant")
 Optional<Account> lock(@Param("id") String id,@Param("tenant") String tenant);
}
interface LedgerRepository extends JpaRepository<LedgerEntry,String>, JpaSpecificationExecutor<LedgerEntry> {
 Optional<LedgerEntry> findByTenantIdAndClientRef(String tenant,String clientRef);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select l from LedgerEntry l where l.id=:id and l.tenantId=:tenant")
 Optional<LedgerEntry> lock(@Param("id") String id,@Param("tenant") String tenant);
 @Query("select coalesce(sum(case when l.kind='INCOME' then l.amount else -l.amount end),0) from LedgerEntry l where l.tenantId=:tenant and l.accountId=:account and l.status='POSTED'")
 BigDecimal balance(@Param("tenant") String tenant,@Param("account") String account);
 @Query("select l.kind, sum(l.baseAmount), count(l) from LedgerEntry l where l.tenantId=:tenant and l.status='POSTED' and l.bookedOn between :start and :end group by l.kind")
 List<Object[]> totals(@Param("tenant") String tenant,@Param("start") LocalDate start,@Param("end") LocalDate end);
 @Query("select year(l.bookedOn),month(l.bookedOn),l.kind,sum(l.baseAmount) from LedgerEntry l where l.tenantId=:tenant and l.status='POSTED' and l.bookedOn between :start and :end group by year(l.bookedOn),month(l.bookedOn),l.kind order by year(l.bookedOn),month(l.bookedOn)")
 List<Object[]> monthly(@Param("tenant") String tenant,@Param("start") LocalDate start,@Param("end") LocalDate end);
 @Query("select l.category, sum(l.baseAmount), count(l) from LedgerEntry l where l.tenantId=:tenant and l.status='POSTED' and l.kind='EXPENSE' and l.bookedOn between :start and :end group by l.category order by sum(l.baseAmount) desc")
 List<Object[]> categories(@Param("tenant") String tenant,@Param("start") LocalDate start,@Param("end") LocalDate end);
}
interface FxRepository extends JpaRepository<FxRate,String> {
 Optional<FxRate> findByBaseCurrencyAndQuoteCurrencyAndRateDateAndSource(String base,String quote,LocalDate date,String source);
 Page<FxRate> findByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(String base,String quote,Pageable page);
}
interface AuditRepository extends JpaRepository<AuditEvent,String> { Page<AuditEvent> findByTenantIdOrderByCreatedAtDesc(String tenant,Pageable page); }
