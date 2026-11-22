package com.monef.controller;

import com.monef.dto.ChatRequest;
import com.monef.dto.ChatResult;
import com.monef.security.TenantAccess;
import com.monef.service.AiService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AiController {

    private final TenantAccess access;
    private final AiService ai;

    public AiController(TenantAccess access, AiService ai) {
        this.access = access;
        this.ai = ai;
    }

    @PostMapping("/ai/chat")
    public ChatResult chat(@Valid @RequestBody ChatRequest request) {
        return ai.chat(access.user(), request);
    }
}
