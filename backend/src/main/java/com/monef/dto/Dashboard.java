package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record Dashboard(
    String month,
    String baseCurrency,
    Metric income,
    Metric expense,
    Metric netCashFlow,
    BigDecimal surplusMargin,
    long transactionCount,
    List<MonthPoint> series,
    List<CategoryPoint> categories,
    Instant generatedAt
) {}
