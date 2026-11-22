package com.monef.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> api(ApiException e) {
        return error(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> credentials() {
        return error(401, "邮箱或密码错误");
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        IllegalArgumentException.class,
    })
    public ResponseEntity<?> invalid(Exception e) {
        return error(400, "请求参数无效，请检查日期、金额和必填字段");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict() {
        return error(409, "请求发生冲突，请使用相同的请求编号重试或刷新数据");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> forbidden() {
        return error(403, "此操作需要管理员权限");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("API request failed", e);
        return error(500, "服务暂时不可用，请稍后重试");
    }

    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(
            Map.of("message", message, "timestamp", Instant.now().toString())
        );
    }
}
