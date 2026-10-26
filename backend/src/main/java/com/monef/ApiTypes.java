package com.monef;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ApiTypes {
 private ApiTypes() {}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank @Size(max=200) String password) {}
 public record UserView(String email,String name,String role,String tenantId,boolean demo) {}
 public record AccountRequest(@NotBlank @Size(max=100) String name,@NotBlank @Size(max=100) String institution,
   @NotBlank String currency,@Pattern(regexp="BANK|CASH|WALLET") String type,
   @NotNull @DecimalMin("0.00") @DecimalMax("9999999999999.99") @Digits(integer=13,fraction=2) BigDecimal openingBalance) {}
 public record AccountView(String id,String name,String institution,String currency,String type,BigDecimal openingBalance,BigDecimal balance,boolean active) {}
 public record EntryRequest(@NotBlank String accountId,@NotNull @Pattern(regexp="INCOME|EXPENSE") String kind,
   @NotNull @DecimalMin("0.01") @DecimalMax("9999999999999.99") @Digits(integer=13,fraction=2) BigDecimal amount,
   @NotBlank @Size(max=200) String description,@NotBlank @Size(max=40) String category,
   @NotNull LocalDate bookedOn,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") String clientRef) {}
 public record EntryView(String id,String accountId,String accountName,String kind,BigDecimal amount,String currency,
   BigDecimal baseAmount,BigDecimal fxRate,LocalDate fxDate,String fxSource,String description,String category,LocalDate bookedOn,String status) {}
 public record PageView<T>(List<T> content,int page,int size,long totalElements,int totalPages) {}
 public record Metric(BigDecimal value,BigDecimal change) {}
 public record MonthPoint(String month,BigDecimal income,BigDecimal expense) {}
 public record CategoryPoint(String category,BigDecimal amount,long count) {}
 public record Dashboard(String month,String baseCurrency,Metric income,Metric expense,Metric netCashFlow,BigDecimal surplusMargin,
   long transactionCount,List<MonthPoint> series,List<CategoryPoint> categories,Instant generatedAt) {}
 public record RateView(String base,String quote,BigDecimal rate,LocalDate date,String source,boolean cached,Instant fetchedAt) {}
 public record Conversion(String base,String quote,BigDecimal amount,BigDecimal converted,RateView observation) {}
 public record ChatRequest(@NotBlank @Size(max=1000) String message,@NotBlank String month) {}
 public record FinanceSnapshot(String month,String baseCurrency,Metric income,Metric expense,Metric netCashFlow,BigDecimal surplusMargin,long transactionCount,List<CategoryPoint> categories,Instant generatedAt) {}
 public record ChatResult(String answer,String mode,String month,String currency,long transactionCount,List<String> tools,Instant generatedAt) {}
}
