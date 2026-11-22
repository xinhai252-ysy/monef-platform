package com.monef.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public record ChatRequest(@NotBlank @Size(max = 1000) String message, @NotBlank String month) {}
