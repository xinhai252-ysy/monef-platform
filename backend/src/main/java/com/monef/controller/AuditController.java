package com.monef.controller;

import com.monef.dto.AuditView;
import com.monef.dto.PageView;
import com.monef.security.TenantAccess;
import com.monef.service.AuditService;
import jakarta.servlet.http.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuditController {

    private final TenantAccess access;
    private final AuditService auditService;

    public AuditController(TenantAccess access, AuditService auditService) {
        this.access = access;
        this.auditService = auditService;
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public PageView<AuditView> audit(@RequestParam(defaultValue = "0") int page) {
        return auditService.list(access.tenant(), page);
    }
}
