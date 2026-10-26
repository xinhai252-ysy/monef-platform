package com.monef;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import java.math.*;
import java.time.*;
import java.util.*;
@Component
class DemoData implements ApplicationRunner {
 private final UserRepository users;private final AccountRepository accounts;private final LedgerRepository entries;private final PasswordEncoder encoder;private final Clock clock;
 private final boolean seed;private final String email,password,tenant;
 DemoData(UserRepository users,AccountRepository accounts,LedgerRepository entries,PasswordEncoder encoder,Clock clock,@Value("${monef.seed-demo}") boolean seed,@Value("${monef.admin-email}") String email,@Value("${monef.admin-password}") String password,@Value("${monef.tenant}") String tenant){this.users=users;this.accounts=accounts;this.entries=entries;this.encoder=encoder;this.clock=clock;this.seed=seed;this.email=email;this.password=password;this.tenant=tenant;}
 @Override @Transactional public void run(ApplicationArguments args) {
  if(email.isBlank()||password.length()<12)throw new IllegalStateException("Set MONEF_ADMIN_EMAIL and MONEF_ADMIN_PASSWORD (12+ characters)");
  if(users.findByEmail(email.toLowerCase(Locale.ROOT)).isEmpty()) {var u=new AppUser();u.email=email.toLowerCase(Locale.ROOT);u.passwordHash=encoder.encode(password);u.displayName=seed?"Alex Morgan":"财务管理员";u.tenantId=tenant;u.userRole="ADMIN";users.save(u);}
  if(!seed||!accounts.findByTenantIdOrderByCreatedAt(tenant).isEmpty())return;
  var usd=account("企业主账户","Mercury ···· 4829","USD","BANK","85000");var eur=account("欧洲业务账户","Wise ···· 7291","EUR","WALLET","28000");var cny=account("人民币账户","招商银行 ···· 9066","CNY","BANK","160000");
  var now=YearMonth.now(clock);var random=new Random(42);
  String[] incomeNames={"Linear · 年度产品订阅","Acme Studio · 项目服务","Notion Labs · 企业授权","Vercel · 技术咨询","Figma · 团队方案"};
  String[] expenseNames={"团队薪资","品牌推广计划","研发设备采购","软件与云服务","办公室租金","差旅及行政"};
  String[] categories={"人力成本","市场营销","研发投入","软件订阅","办公运营","其他支出"};
  for(int back=11;back>=0;back--){var month=now.minusMonths(back);int maxDay=back==0?LocalDate.now(clock).getDayOfMonth():month.lengthOfMonth();
   for(int i=0;i<20;i++) {boolean in=i%3==0;var a=i%8==0?eur:i%11==0?cny:usd;var date=month.atDay(1+(i*7)%maxDay);int growth=12-back;
    BigDecimal base=BigDecimal.valueOf(in?12000+random.nextInt(15000)+growth*900:1800+random.nextInt(4500)+growth*120);
    BigDecimal toUsd=a.currency.equals("EUR")?new BigDecimal("1.0869565217"):a.currency.equals("CNY")?new BigDecimal("0.1404494382"):BigDecimal.ONE;
    var e=new LedgerEntry();e.tenantId=tenant;e.accountId=a.id;e.kind=in?"INCOME":"EXPENSE";e.currency=a.currency;e.fxRate=toUsd;e.amount=base.divide(toUsd,2,RoundingMode.HALF_UP);e.baseAmount=e.amount.multiply(toUsd).setScale(2,RoundingMode.HALF_UP);e.fxDate=date;e.fxSource="DEMO";e.bookedOn=date;e.description=in?incomeNames[i%5]:expenseNames[i%6];e.category=in?(i%2==0?"产品收入":"服务收入"):categories[i%6];e.status="POSTED";e.clientRef="seed_"+month+"_"+i;e.createdAt=date.atStartOfDay().toInstant(ZoneOffset.UTC);entries.save(e);
   }
  }
 }
 private Account account(String name,String institution,String currency,String type,String balance){var a=new Account();a.tenantId=tenant;a.name=name;a.institution=institution;a.currency=currency;a.accountType=type;a.openingBalance=new BigDecimal(balance);return accounts.save(a);}
}
