package com.monef;
import static com.monef.ApiTypes.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api")
class ApiController {
 private final AuthenticationManager auth;private final SecurityContextRepository context;private final TenantAccess access;private final FinanceService finance;private final DashboardService dashboards;private final FxService fx;private final AiService ai;private final RateLimiter limiter;private final AuditRepository audits;private final boolean demo;
 ApiController(AuthenticationManager auth,SecurityContextRepository context,TenantAccess access,FinanceService finance,DashboardService dashboards,FxService fx,AiService ai,RateLimiter limiter,AuditRepository audits,@Value("${monef.seed-demo}") boolean demo){this.auth=auth;this.context=context;this.access=access;this.finance=finance;this.dashboards=dashboards;this.fx=fx;this.ai=ai;this.limiter=limiter;this.audits=audits;this.demo=demo;}
 @GetMapping("/auth/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @PostMapping("/auth/login") UserView login(@Valid @RequestBody LoginRequest r,HttpServletRequest req,HttpServletResponse res){
  limiter.check("login:"+req.getRemoteAddr(),20,60);
  var authentication=auth.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(r.email().toLowerCase(Locale.ROOT),r.password()));
  req.getSession(true);req.changeSessionId();var c=SecurityContextHolder.createEmptyContext();c.setAuthentication(authentication);SecurityContextHolder.setContext(c);context.saveContext(c,req,res);return me();
 }
 @GetMapping("/auth/me") UserView me(){var u=access.user();return new UserView(u.email,u.displayName,u.userRole,u.tenantId,demo);}
 @GetMapping("/accounts") List<AccountView> accounts(){return finance.accounts(access.tenant());}
 @PostMapping("/accounts") @PreAuthorize("hasRole('ADMIN')") AccountView createAccount(@Valid @RequestBody AccountRequest r){return finance.createAccount(access.user(),r);}
 @PatchMapping("/accounts/{id}/status") @PreAuthorize("hasRole('ADMIN')") AccountView status(@PathVariable String id,@RequestBody StatusRequest r){return finance.setActive(access.user(),id,r.active());}
 record StatusRequest(boolean active) {}
 @GetMapping("/transactions") PageView<EntryView> transactions(@RequestParam(required=false) String q,@RequestParam(required=false) String kind,@RequestParam(required=false) String status,@RequestParam(required=false) String accountId,@RequestParam(required=false) LocalDate start,@RequestParam(required=false) LocalDate end,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return finance.list(access.tenant(),q,kind,status,accountId,start,end,page,size);}
 @PostMapping("/transactions") @PreAuthorize("hasRole('ADMIN')") EntryView create(@Valid @RequestBody EntryRequest r){return finance.create(access.user(),r);}
 @PostMapping("/transactions/{id}/void") @PreAuthorize("hasRole('ADMIN')") EntryView voidEntry(@PathVariable String id){return finance.voidEntry(access.user(),id);}
 @GetMapping("/dashboard") Dashboard dashboard(@RequestParam String month){return dashboards.dashboard(access.tenant(),month);}
 @GetMapping("/fx/rate") RateView rate(@RequestParam String base,@RequestParam String quote,@RequestParam(required=false) LocalDate date){return fx.rate(base,quote,date);}
 @GetMapping("/fx/convert") Conversion convert(@RequestParam String base,@RequestParam String quote,@RequestParam BigDecimal amount,@RequestParam(required=false) LocalDate date){return fx.convert(base,quote,amount,date);}
 @GetMapping("/fx/history") PageView<RateView> history(@RequestParam String base,@RequestParam String quote,@RequestParam(defaultValue="0") int page){return fx.history(base,quote,page);}
 @GetMapping("/currencies") List<String> currencies(){return FxService.CURRENCIES.stream().sorted().toList();}
 @GetMapping("/categories") List<String> categories(){return FinanceService.CATEGORIES.stream().sorted().toList();}
 @PostMapping("/ai/chat") ChatResult chat(@Valid @RequestBody ChatRequest r){return ai.chat(access.user(),r);}
 @GetMapping("/audit") @PreAuthorize("hasRole('ADMIN')") PageView<AuditView> audit(@RequestParam(defaultValue="0") int page){if(page<0)throw new ApiException(400,"页码不能为负");var p=audits.findByTenantIdOrderByCreatedAtDesc(access.tenant(),PageRequest.of(page,20));return new PageView<>(p.map(a->new AuditView(a.id,a.actor,a.action,a.subjectId,a.detail,a.createdAt)).getContent(),page,20,p.getTotalElements(),p.getTotalPages());}
 record AuditView(String id,String actor,String action,String subjectId,String detail,Instant createdAt) {}
}
