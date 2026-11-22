package com.monef.controller;

import com.monef.dto.AccountRequest;
import com.monef.dto.AccountView;
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
public class AccountController {

    private final TenantAccess access;
    private final FinanceService finance;

    public AccountController(TenantAccess access, FinanceService finance) {
        this.access = access;
        this.finance = finance;
    }

    @GetMapping("/accounts")
    public List<AccountView> accounts() {
        return finance.accounts(access.tenant());
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountView createAccount(@Valid @RequestBody AccountRequest request) {
        return finance.createAccount(access.user(), request);
    }

    @PatchMapping("/accounts/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountView status(@PathVariable String id, @RequestBody StatusRequest request) {
        return finance.setActive(access.user(), id, request.active());
    }

    public record StatusRequest(boolean active) {}
}
