package com.monef.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 200) String password
) {}
