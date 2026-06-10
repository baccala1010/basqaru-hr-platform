package kz.whosnext.hr.candidate.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(body(400, "Ошибка валидации", req, errors));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex, WebRequest req) {
        return ResponseEntity.badRequest().body(body(400, ex.getMessage(), req, null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, WebRequest req) {
        return ResponseEntity.status(404).body(body(404, ex.getMessage(), req, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, WebRequest req) {
        return ResponseEntity.status(403).body(body(403, ex.getMessage(), req, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, WebRequest req) {
        log.error("Unhandled exception at {}: {}", req.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(500).body(body(500, "Внутренняя ошибка сервера", req, null));
    }

    private Map<String, Object> body(int status, String message, WebRequest req, Map<String, String> validationErrors) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("message", message);
        body.put("path", req.getDescription(false).replace("uri=", ""));
        body.put("timestamp", LocalDateTime.now().toString());
        if (validationErrors != null) body.put("validationErrors", validationErrors);
        return body;
    }
}

