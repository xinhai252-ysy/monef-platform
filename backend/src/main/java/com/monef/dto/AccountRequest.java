package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AccountRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 100) String institution,
    @NotBlank String currency,
    @Pattern(regexp = "BANK|CASH|WALLET") String type,
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("9999999999999.99")
    @Digits(integer = 13, fraction = 2)
    BigDecimal openingBalance
) {}
