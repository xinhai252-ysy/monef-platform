package com.monef.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public record UserView(String email, String name, String role, String tenantId, boolean demo) {}
