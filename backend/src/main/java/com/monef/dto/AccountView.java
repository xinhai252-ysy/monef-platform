package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AccountView(
    String id,
    String name,
    String institution,
    String currency,
    String type,
    BigDecimal openingBalance,
    BigDecimal balance,
    boolean active
) {}
