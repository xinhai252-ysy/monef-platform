package com.monef;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="app_user")
class AppUser {
 @Id String id = UUID.randomUUID().toString();
 String tenantId, email, passwordHash, displayName, userRole;
}
@Entity @Table(name="finance_account")
class Account {
 @Id String id = UUID.randomUUID().toString();
 String tenantId, name, institution, currency, accountType;
 @Column(precision=19,scale=2) BigDecimal openingBalance;
 boolean active = true;
 @Version long version;
 Instant createdAt = Instant.now();
}
@Entity @Table(name="ledger_entry")
class LedgerEntry {
 @Id String id = UUID.randomUUID().toString();
 String tenantId, accountId, kind, currency, description, category, status, clientRef, fxSource;
 @Column(precision=19,scale=2) BigDecimal amount;
 @Column(precision=19,scale=2) BigDecimal baseAmount;
 @Column(precision=24,scale=10) BigDecimal fxRate;
 LocalDate fxDate, bookedOn;
 Instant createdAt = Instant.now();
}
@Entity @Table(name="fx_rate")
class FxRate {
 @Id String id = UUID.randomUUID().toString();
 String baseCurrency, quoteCurrency, source;
 LocalDate rateDate;
 @Column(precision=24,scale=10) BigDecimal rate;
 Instant fetchedAt = Instant.now();
}
@Entity @Table(name="audit_event")
class AuditEvent {
 @Id String id = UUID.randomUUID().toString();
 String tenantId, actor, action, subjectId, detail;
 Instant createdAt = Instant.now();
}
