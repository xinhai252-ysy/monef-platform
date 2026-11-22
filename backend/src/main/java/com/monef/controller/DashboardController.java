package com.monef.controller;

import com.monef.dto.Dashboard;
import com.monef.security.TenantAccess;
import com.monef.service.DashboardService;
import jakarta.servlet.http.*;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final TenantAccess access;
    private final DashboardService dashboards;

    public DashboardController(TenantAccess access, DashboardService dashboards) {
        this.access = access;
        this.dashboards = dashboards;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam String month) {
        return dashboards.dashboard(access.tenant(), month);
    }
}
