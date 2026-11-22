package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record EntryRequest(
    @NotBlank String accountId,
    @NotNull @Pattern(regexp = "INCOME|EXPENSE") String kind,
    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("9999999999999.99")
    @Digits(integer = 13, fraction = 2)
    BigDecimal amount,
    @NotBlank @Size(max = 200) String description,
    @NotBlank @Size(max = 40) String category,
    @NotNull LocalDate bookedOn,
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{8,64}") String clientRef
) {}
