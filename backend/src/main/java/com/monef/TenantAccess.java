package com.monef;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
@Component
class TenantAccess {
 private final UserRepository users;
 TenantAccess(UserRepository users) { this.users=users; }
 AppUser user() {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth==null) throw new ApiException(401,"请先登录");
  return users.findByEmail(auth.getName()).orElseThrow(() -> new ApiException(401,"用户不存在"));
 }
 String tenant() { return user().tenantId; }
}
