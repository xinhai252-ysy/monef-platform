package com.monef.dto;

import java.time.Instant;

public record AuditView(
    String id,
    String actor,
    String action,
    String subjectId,
    String detail,
    Instant createdAt
) {}
