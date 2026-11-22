package com.monef.service;

import com.monef.dto.AccountRequest;
import com.monef.dto.AccountView;
import com.monef.dto.EntryRequest;
import com.monef.dto.EntryView;
import com.monef.dto.PageView;
import com.monef.entity.Account;
import com.monef.entity.AppUser;
import com.monef.entity.AuditEvent;
import com.monef.entity.LedgerEntry;
import com.monef.exception.ApiException;
import com.monef.repository.AccountRepository;
import com.monef.repository.AuditRepository;
import com.monef.repository.LedgerRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {

    public static final Set<String> CATEGORIES = Set.of(
        "产品收入",
        "服务收入",
        "其他收入",
        "人力成本",
        "市场营销",
        "研发投入",
        "软件订阅",
        "办公运营",
        "其他支出"
    );
    private final AccountRepository accounts;
    private final LedgerRepository entries;
    private final AuditRepository audits;
    private final FxService fx;
    private final Clock clock;

    public FinanceService(
        AccountRepository accounts,
        LedgerRepository entries,
        AuditRepository audits,
        FxService fx,
        Clock clock
    ) {
        this.accounts = accounts;
        this.entries = entries;
        this.audits = audits;
        this.fx = fx;
        this.clock = clock;
    }

    public List<AccountView> accounts(String tenant) {
        return accounts
            .findByTenantIdOrderByCreatedAt(tenant)
            .stream()
            .map(a -> accountView(a, tenant))
            .toList();
    }

    public AccountView accountView(Account a, String tenant) {
        return new AccountView(
            a.getId(),
            a.getName(),
            a.getInstitution(),
            a.getCurrency(),
            a.getAccountType(),
            a.getOpeningBalance(),
            a.getOpeningBalance().add(entries.balance(tenant, a.getId())),
            a.isActive()
        );
    }

    @Transactional
    public AccountView createAccount(AppUser user, AccountRequest r) {
        if (r.type() == null) throw new ApiException(400, "请选择账户类型");
        var a = new Account();
        a.setTenantId(user.getTenantId());
        a.setName(r.name().trim());
        a.setInstitution(r.institution().trim());
        a.setCurrency(fx.currency(r.currency()));
        a.setAccountType(r.type());
        a.setOpeningBalance(r.openingBalance().setScale(2));
        a.setCreatedAt(clock.instant());
        accounts.save(a);
        audit(user, "ACCOUNT_CREATED", a.getId(), a.getCurrency());
        return accountView(a, user.getTenantId());
    }

    @Transactional
    public AccountView setActive(AppUser user, String id, boolean active) {
        var a = accounts
            .lock(id, user.getTenantId())
            .orElseThrow(() -> new ApiException(404, "账户不存在"));
        a.setActive(active);
        audit(user, active ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED", id, "Status changed");
        return accountView(a, user.getTenantId());
    }

    @Transactional
    public EntryView create(AppUser user, EntryRequest r) {
        validateDate(r.bookedOn());
        if (!CATEGORIES.contains(r.category())) throw new ApiException(400, "请选择有效分类");
        if (
            (!r.category().endsWith("收入") && "INCOME".equals(r.kind())) ||
            (r.category().endsWith("收入") && "EXPENSE".equals(r.kind()))
        ) throw new ApiException(400, "分类与收支类型不匹配");
        var account = accounts
            .lock(r.accountId(), user.getTenantId())
            .orElseThrow(() -> new ApiException(404, "账户不存在"));
        var existing = entries.findByTenantIdAndClientRef(user.getTenantId(), r.clientRef());
        if (existing.isPresent()) {
            var e = existing.get();
            if (!same(e, r)) throw new ApiException(409, "同一请求编号不能用于不同的交易");
            return entryView(e, account.getName());
        }
        if (!account.isActive()) throw new ApiException(409, "已停用账户不能新增交易");
        var rate = fx.rate(account.getCurrency(), "USD", r.bookedOn());
        var e = new LedgerEntry();
        e.setTenantId(user.getTenantId());
        e.setAccountId(account.getId());
        e.setKind(r.kind());
        e.setAmount(r.amount().setScale(2));
        e.setCurrency(account.getCurrency());
        e.setFxRate(rate.rate());
        e.setFxDate(rate.date());
        e.setFxSource(rate.source());
        e.setBaseAmount(e.getAmount().multiply(e.getFxRate()).setScale(2, RoundingMode.HALF_UP));
        if (e.getBaseAmount().precision() - e.getBaseAmount().scale() > 17) throw new ApiException(
            400,
            "折算金额超出系统范围"
        );
        e.setDescription(r.description().trim());
        e.setCategory(r.category());
        e.setBookedOn(r.bookedOn());
        e.setStatus("POSTED");
        e.setClientRef(r.clientRef());
        e.setCreatedAt(clock.instant());
        entries.saveAndFlush(e);
        audit(
            user,
            "TRANSACTION_CREATED",
            e.getId(),
            e.getKind() + " " + e.getCurrency() + " " + e.getAmount()
        );
        return entryView(e, account.getName());
    }

    private boolean same(LedgerEntry e, EntryRequest r) {
        return (
            e.getAccountId().equals(r.accountId()) &&
            e.getKind().equals(r.kind()) &&
            e.getAmount().compareTo(r.amount()) == 0 &&
            e.getDescription().equals(r.description().trim()) &&
            e.getCategory().equals(r.category()) &&
            e.getBookedOn().equals(r.bookedOn())
        );
    }

    @Transactional
    public EntryView voidEntry(AppUser user, String id) {
        var e = entries
            .lock(id, user.getTenantId())
            .orElseThrow(() -> new ApiException(404, "交易不存在"));
        if (!"VOID".equals(e.getStatus())) {
            e.setStatus("VOID");
            audit(user, "TRANSACTION_VOIDED", id, "Excluded from balances and analytics");
        }
        return entryView(
            e,
            accounts
                .findByIdAndTenantId(e.getAccountId(), user.getTenantId())
                .orElseThrow()
                .getName()
        );
    }

    public PageView<EntryView> list(
        String tenant,
        String query,
        String kind,
        String status,
        String accountId,
        LocalDate start,
        LocalDate end,
        int page,
        int size
    ) {
        if (page < 0 || size < 1 || size > 100) throw new ApiException(
            400,
            "分页大小必须介于 1 与 100"
        );
        if (start != null && end != null && start.isAfter(end)) throw new ApiException(
            400,
            "开始日期不能晚于结束日期"
        );
        if (kind != null && !Set.of("INCOME", "EXPENSE").contains(kind)) throw new ApiException(
            400,
            "收支类型无效"
        );
        if (status != null && !Set.of("POSTED", "VOID").contains(status)) throw new ApiException(
            400,
            "交易状态无效"
        );
        if (query != null && query.length() > 100) throw new ApiException(400, "搜索词过长");
        Specification<LedgerEntry> filter = (root, q, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenant));
            if (query != null && !query.isBlank()) {
                String search =
                    "%" +
                    query
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_") +
                    "%";
                predicates.add(cb.like(cb.lower(root.get("description")), search, '\\'));
            }
            if (kind != null) predicates.add(cb.equal(root.get("kind"), kind));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (accountId != null) predicates.add(cb.equal(root.get("accountId"), accountId));
            if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("bookedOn"), start));
            if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("bookedOn"), end));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var p = entries.findAll(
            filter,
            PageRequest.of(
                page,
                size,
                Sort.by(
                    Sort.Order.desc("bookedOn"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
                )
            )
        );
        var names = new HashMap<String, String>();
        accounts
            .findByTenantIdOrderByCreatedAt(tenant)
            .forEach(a -> names.put(a.getId(), a.getName()));
        return new PageView<>(
            p.map(e -> entryView(e, names.get(e.getAccountId()))).getContent(),
            page,
            size,
            p.getTotalElements(),
            p.getTotalPages()
        );
    }

    static EntryView entryView(LedgerEntry e, String name) {
        return new EntryView(
            e.getId(),
            e.getAccountId(),
            name,
            e.getKind(),
            e.getAmount(),
            e.getCurrency(),
            e.getBaseAmount(),
            e.getFxRate(),
            e.getFxDate(),
            e.getFxSource(),
            e.getDescription(),
            e.getCategory(),
            e.getBookedOn(),
            e.getStatus()
        );
    }

    public void audit(AppUser user, String action, String subject, String detail) {
        var a = new AuditEvent();
        a.setTenantId(user.getTenantId());
        a.setActor(user.getEmail());
        a.setAction(action);
        a.setSubjectId(subject);
        a.setDetail(detail);
        a.setCreatedAt(clock.instant());
        audits.save(a);
    }

    private void validateDate(LocalDate date) {
        if (
            date.isAfter(LocalDate.now(clock)) || date.isBefore(LocalDate.of(2000, 1, 1))
        ) throw new ApiException(400, "记账日期需介于 2000-01-01 与今天之间");
    }
}
