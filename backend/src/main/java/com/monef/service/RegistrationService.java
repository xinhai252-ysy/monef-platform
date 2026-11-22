package com.monef.service;

import com.monef.dto.RegisterRequest;
import com.monef.entity.AppUser;
import com.monef.entity.AuditEvent;
import com.monef.exception.ApiException;
import com.monef.repository.AuditRepository;
import com.monef.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final AuditRepository audits;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
        UserRepository users,
        AuditRepository audits,
        PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.audits = audits;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(400, "密码的 UTF-8 长度不能超过 72 字节，请减少字符数");
        }
        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(409, "该邮箱已注册，请直接登录");
        }

        var user = new AppUser();
        user.setEmail(email);
        user.setDisplayName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setTenantId(UUID.randomUUID().toString());
        user.setUserRole("ADMIN");
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "该邮箱已注册，请直接登录");
        }

        var event = new AuditEvent();
        event.setTenantId(user.getTenantId());
        event.setActor(email);
        event.setAction("USER_REGISTERED");
        event.setSubjectId(user.getId());
        event.setDetail("创建独立工作区");
        audits.save(event);
    }
}
