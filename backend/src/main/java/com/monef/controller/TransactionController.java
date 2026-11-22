package com.monef.controller;

import com.monef.dto.EntryRequest;
import com.monef.dto.EntryView;
import com.monef.dto.PageView;
import com.monef.security.TenantAccess;
import com.monef.service.FinanceService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TenantAccess access;
    private final FinanceService finance;

    public TransactionController(TenantAccess access, FinanceService finance) {
        this.access = access;
        this.finance = finance;
    }

    @GetMapping("/transactions")
    public PageView<EntryView> transactions(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String kind,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String accountId,
        @RequestParam(required = false) LocalDate start,
        @RequestParam(required = false) LocalDate end,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return finance.list(access.tenant(), q, kind, status, accountId, start, end, page, size);
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public EntryView create(@Valid @RequestBody EntryRequest request) {
        return finance.create(access.user(), request);
    }

    @PostMapping("/transactions/{id}/void")
    @PreAuthorize("hasRole('ADMIN')")
    public EntryView voidEntry(@PathVariable String id) {
        return finance.voidEntry(access.user(), id);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return FinanceService.CATEGORIES.stream().sorted().toList();
    }
}
