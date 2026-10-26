package com.monef;
import static com.monef.ApiTypes.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.*;
import java.math.*;
import java.util.*;

@Service
class FinanceService {
 static final Set<String> CATEGORIES=Set.of("产品收入","服务收入","其他收入","人力成本","市场营销","研发投入","软件订阅","办公运营","其他支出");
 private final AccountRepository accounts;private final LedgerRepository entries;private final AuditRepository audits;private final FxService fx;private final Clock clock;
 FinanceService(AccountRepository accounts,LedgerRepository entries,AuditRepository audits,FxService fx,Clock clock) {this.accounts=accounts;this.entries=entries;this.audits=audits;this.fx=fx;this.clock=clock;}
 List<AccountView> accounts(String tenant) {return accounts.findByTenantIdOrderByCreatedAt(tenant).stream().map(a -> accountView(a,tenant)).toList();}
 AccountView accountView(Account a,String tenant) {return new AccountView(a.id,a.name,a.institution,a.currency,a.accountType,a.openingBalance,a.openingBalance.add(entries.balance(tenant,a.id)),a.active);}
 @Transactional AccountView createAccount(AppUser user,AccountRequest r) {
  if(r.type()==null)throw new ApiException(400,"请选择账户类型");
  var a=new Account();a.tenantId=user.tenantId;a.name=r.name().trim();a.institution=r.institution().trim();a.currency=fx.currency(r.currency());a.accountType=r.type();a.openingBalance=r.openingBalance().setScale(2);a.createdAt=clock.instant();accounts.save(a);
  audit(user,"ACCOUNT_CREATED",a.id,a.currency);return accountView(a,user.tenantId);
 }
 @Transactional AccountView setActive(AppUser user,String id,boolean active) {
  var a=accounts.lock(id,user.tenantId).orElseThrow(() -> new ApiException(404,"账户不存在"));a.active=active;
  audit(user,active?"ACCOUNT_ENABLED":"ACCOUNT_DISABLED",id,"Status changed");return accountView(a,user.tenantId);
 }
 @Transactional EntryView create(AppUser user,EntryRequest r) {
  validateDate(r.bookedOn());if(!CATEGORIES.contains(r.category()))throw new ApiException(400,"请选择有效分类");
  if(!r.category().endsWith("收入")&&"INCOME".equals(r.kind())||r.category().endsWith("收入")&&"EXPENSE".equals(r.kind()))throw new ApiException(400,"分类与收支类型不匹配");
  var account=accounts.lock(r.accountId(),user.tenantId).orElseThrow(() -> new ApiException(404,"账户不存在"));
  var existing=entries.findByTenantIdAndClientRef(user.tenantId,r.clientRef());
  if(existing.isPresent()) {var e=existing.get();if(!same(e,r))throw new ApiException(409,"同一请求编号不能用于不同的交易");return entryView(e,account.name);}
  if(!account.active)throw new ApiException(409,"已停用账户不能新增交易");
  var rate=fx.rate(account.currency,"USD",r.bookedOn());
  var e=new LedgerEntry();e.tenantId=user.tenantId;e.accountId=account.id;e.kind=r.kind();e.amount=r.amount().setScale(2);e.currency=account.currency;
  e.fxRate=rate.rate();e.fxDate=rate.date();e.fxSource=rate.source();e.baseAmount=e.amount.multiply(e.fxRate).setScale(2,RoundingMode.HALF_UP);
  if(e.baseAmount.precision()-e.baseAmount.scale()>17)throw new ApiException(400,"折算金额超出系统范围");
  e.description=r.description().trim();e.category=r.category();e.bookedOn=r.bookedOn();e.status="POSTED";e.clientRef=r.clientRef();e.createdAt=clock.instant();entries.saveAndFlush(e);
  audit(user,"TRANSACTION_CREATED",e.id,e.kind+" "+e.currency+" "+e.amount);return entryView(e,account.name);
 }
 private boolean same(LedgerEntry e,EntryRequest r) {return e.accountId.equals(r.accountId())&&e.kind.equals(r.kind())&&e.amount.compareTo(r.amount())==0&&e.description.equals(r.description().trim())&&e.category.equals(r.category())&&e.bookedOn.equals(r.bookedOn());}
 @Transactional EntryView voidEntry(AppUser user,String id) {
  var e=entries.lock(id,user.tenantId).orElseThrow(() -> new ApiException(404,"交易不存在"));
  if(!"VOID".equals(e.status)){e.status="VOID";audit(user,"TRANSACTION_VOIDED",id,"Excluded from balances and analytics");}
  return entryView(e,accounts.findByIdAndTenantId(e.accountId,user.tenantId).orElseThrow().name);
 }
 PageView<EntryView> list(String tenant,String query,String kind,String status,String accountId,LocalDate start,LocalDate end,int page,int size) {
  if(page<0||size<1||size>100)throw new ApiException(400,"分页大小必须介于 1 与 100");
  if(start!=null&&end!=null&&start.isAfter(end))throw new ApiException(400,"开始日期不能晚于结束日期");
  if(kind!=null&&!Set.of("INCOME","EXPENSE").contains(kind))throw new ApiException(400,"收支类型无效");
  if(status!=null&&!Set.of("POSTED","VOID").contains(status))throw new ApiException(400,"交易状态无效");
  if(query!=null&&query.length()>100)throw new ApiException(400,"搜索词过长");
  Specification<LedgerEntry> filter=(root,q,cb)->{
   var predicates=new ArrayList<Predicate>();predicates.add(cb.equal(root.get("tenantId"),tenant));
   if(query!=null&&!query.isBlank()) {String search="%"+query.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";predicates.add(cb.like(cb.lower(root.get("description")),search,'\\'));}
   if(kind!=null)predicates.add(cb.equal(root.get("kind"),kind));if(status!=null)predicates.add(cb.equal(root.get("status"),status));
   if(accountId!=null)predicates.add(cb.equal(root.get("accountId"),accountId));if(start!=null)predicates.add(cb.greaterThanOrEqualTo(root.get("bookedOn"),start));if(end!=null)predicates.add(cb.lessThanOrEqualTo(root.get("bookedOn"),end));
   return cb.and(predicates.toArray(Predicate[]::new));
  };
  var p=entries.findAll(filter,PageRequest.of(page,size,Sort.by(Sort.Order.desc("bookedOn"),Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
  var names=new HashMap<String,String>();accounts.findByTenantIdOrderByCreatedAt(tenant).forEach(a->names.put(a.id,a.name));
  return new PageView<>(p.map(e->entryView(e,names.get(e.accountId))).getContent(),page,size,p.getTotalElements(),p.getTotalPages());
 }
 static EntryView entryView(LedgerEntry e,String name) {return new EntryView(e.id,e.accountId,name,e.kind,e.amount,e.currency,e.baseAmount,e.fxRate,e.fxDate,e.fxSource,e.description,e.category,e.bookedOn,e.status);}
 void audit(AppUser user,String action,String subject,String detail) {var a=new AuditEvent();a.tenantId=user.tenantId;a.actor=user.email;a.action=action;a.subjectId=subject;a.detail=detail;a.createdAt=clock.instant();audits.save(a);}
 private void validateDate(LocalDate date) {if(date.isAfter(LocalDate.now(clock))||date.isBefore(LocalDate.of(2000,1,1)))throw new ApiException(400,"记账日期需介于 2000-01-01 与今天之间");}
}
