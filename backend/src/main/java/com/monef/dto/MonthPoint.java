package com.monef.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record MonthPoint(String month, BigDecimal income, BigDecimal expense) {}
