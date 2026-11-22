package com.monef.config;

import com.monef.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService users(UserRepository repo) {
        return email ->
            repo
                .findByEmail(email.toLowerCase(java.util.Locale.ROOT))
                .map(u ->
                    User.withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles(u.getUserRole())
                        .build()
                )
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean
    public AuthenticationManager authenticationManager(
        UserDetailsService users,
        PasswordEncoder encoder
    ) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain security(HttpSecurity http, SecurityContextRepository context)
        throws Exception {
        return http
            .securityContext(c -> c.securityContextRepository(context))
            .authorizeHttpRequests(a ->
                a
                    .requestMatchers(
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/favicon.ico",
                        "/api/auth/csrf",
                        "/api/auth/login",
                        "/api/auth/register",
                        "/actuator/health",
                        "/error"
                    )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            .exceptionHandling(e ->
                e
                    .authenticationEntryPoint((req, res, ex) -> {
                        res.setStatus(401);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"message\":\"请先登录\"}");
                    })
                    .accessDeniedHandler((req, res, ex) -> {
                        res.setStatus(403);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write(
                            "{\"message\":\"权限不足或安全令牌已过期，请刷新后重试\"}"
                        );
                    })
            )
            .logout(l ->
                l
                    .logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID", "SESSION")
                    .logoutSuccessHandler((req, res, auth) ->
                        res.setStatus(HttpStatus.NO_CONTENT.value())
                    )
            )
            .build();
    }
}
