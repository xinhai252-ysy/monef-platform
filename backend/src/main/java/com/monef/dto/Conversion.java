package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record Conversion(
    String base,
    String quote,
    BigDecimal amount,
    BigDecimal converted,
    RateView observation
) {}
