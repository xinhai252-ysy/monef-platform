package com.monef.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public record ChatResult(
    String answer,
    String mode,
    String month,
    String currency,
    long transactionCount,
    List<String> tools,
    Instant generatedAt
) {}
