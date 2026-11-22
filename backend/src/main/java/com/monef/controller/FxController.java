package com.monef.controller;

import com.monef.dto.Conversion;
import com.monef.dto.PageView;
import com.monef.dto.RateView;
import com.monef.security.RateLimiter;
import com.monef.security.TenantAccess;
import com.monef.service.FxService;
import jakarta.servlet.http.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FxController {

    private final FxService fx;
    private final RateLimiter limiter;
    private final TenantAccess access;

    public FxController(FxService fx, RateLimiter limiter, TenantAccess access) {
        this.fx = fx;
        this.limiter = limiter;
        this.access = access;
    }

    @GetMapping("/fx/rate")
    public RateView rate(
        @RequestParam String base,
        @RequestParam String quote,
        @RequestParam(required = false) LocalDate date
    ) {
        return fx.rate(base, quote, date);
    }

    @GetMapping("/fx/convert")
    public Conversion convert(
        @RequestParam String base,
        @RequestParam String quote,
        @RequestParam BigDecimal amount,
        @RequestParam(required = false) LocalDate date
    ) {
        limiter.check("fx:convert:" + access.user().getId(), 20, 60);
        return fx.convert(base, quote, amount, date);
    }

    @GetMapping("/fx/history")
    public PageView<RateView> history(
        @RequestParam String base,
        @RequestParam String quote,
        @RequestParam(defaultValue = "0") int page
    ) {
        return fx.history(base, quote, page);
    }

    @GetMapping("/currencies")
    public List<String> currencies() {
        return FxService.CURRENCIES.stream().sorted().toList();
    }
}
