package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record RateView(
    String base,
    String quote,
    BigDecimal rate,
    LocalDate date,
    String source,
    boolean cached,
    Instant fetchedAt,
    Instant quotedAt,
    Integer delaySeconds,
    String providerSource,
    String marketSession,
    boolean stale
) {
    public RateView(
        String base,
        String quote,
        BigDecimal rate,
        LocalDate date,
        String source,
        boolean cached,
        Instant fetchedAt,
        Instant quotedAt
    ) {
        this(base, quote, rate, date, source, cached, fetchedAt, quotedAt, null, null, null, false);
    }

    public RateView(
        String base,
        String quote,
        BigDecimal rate,
        LocalDate date,
        String source,
        boolean cached,
        Instant fetchedAt
    ) {
        this(base, quote, rate, date, source, cached, fetchedAt, null);
    }
}
