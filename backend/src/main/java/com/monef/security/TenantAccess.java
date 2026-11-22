package com.monef.security;

import com.monef.entity.AppUser;
import com.monef.exception.ApiException;
import com.monef.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantAccess {

    private final UserRepository users;

    public TenantAccess(UserRepository users) {
        this.users = users;
    }

    public AppUser user() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new ApiException(401, "请先登录");
        return users
            .findByEmail(auth.getName())
            .orElseThrow(() -> new ApiException(401, "用户不存在"));
    }

    public String tenant() {
        return user().getTenantId();
    }
}
