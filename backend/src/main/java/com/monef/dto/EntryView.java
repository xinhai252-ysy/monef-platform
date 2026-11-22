package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record EntryView(
    String id,
    String accountId,
    String accountName,
    String kind,
    BigDecimal amount,
    String currency,
    BigDecimal baseAmount,
    BigDecimal fxRate,
    LocalDate fxDate,
    String fxSource,
    String description,
    String category,
    LocalDate bookedOn,
    String status
) {}
