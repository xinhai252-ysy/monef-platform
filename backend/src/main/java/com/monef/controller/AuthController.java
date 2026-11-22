package com.monef.controller;

import com.monef.dto.LoginRequest;
import com.monef.dto.RegisterRequest;
import com.monef.dto.UserView;
import com.monef.security.RateLimiter;
import com.monef.security.TenantAccess;
import com.monef.service.RegistrationService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager auth;
    private final SecurityContextRepository context;
    private final TenantAccess access;
    private final RateLimiter limiter;
    private final RegistrationService registration;
    private final boolean demo;

    public AuthController(
        AuthenticationManager auth,
        SecurityContextRepository context,
        TenantAccess access,
        RateLimiter limiter,
        RegistrationService registration,
        @Value("${monef.seed-demo}") boolean demo
    ) {
        this.auth = auth;
        this.context = context;
        this.access = access;
        this.limiter = limiter;
        this.registration = registration;
        this.demo = demo;
    }

    @GetMapping("/auth/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/auth/login")
    public UserView login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        limiter.check("login:" + httpRequest.getRemoteAddr(), 20, 60);
        var authentication = auth.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequest.email().toLowerCase(Locale.ROOT),
                loginRequest.password()
            )
        );
        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        context.saveContext(securityContext, httpRequest, httpResponse);
        return me();
    }

    @GetMapping("/auth/me")
    public UserView me() {
        var currentUser = access.user();
        return new UserView(
            currentUser.getEmail(),
            currentUser.getDisplayName(),
            currentUser.getUserRole(),
            currentUser.getTenantId(),
            demo
        );
    }

    @PostMapping("/auth/register")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public UserView register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        limiter.check("register:" + httpRequest.getRemoteAddr(), 5, 60);
        registration.register(request);
        return login(
            new LoginRequest(request.email().trim(), request.password()),
            httpRequest,
            httpResponse
        );
    }
}
