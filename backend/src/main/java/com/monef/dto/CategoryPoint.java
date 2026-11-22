package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record CategoryPoint(String category, BigDecimal amount, long count) {}
