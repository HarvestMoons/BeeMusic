package com.example.musicplayer.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason() == null ? "请求失败" : ex.getReason()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidRequest() {
        return ResponseEntity.badRequest().body(Map.of("message", "请求参数格式不正确"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "数据冲突，请刷新后重试"));
    }

    public static class ErrorMessageConstants {
        public static final String MESSAGE = "message";
        public static final String DATABASE_ERROR = "数据库连接异常，请稍后重试";
        public static final String DATA_FORMAT_ERROR = "数据格式错误: ";
        public static final String AUTHENTICATION_ERROR = "用户名或密码错误。";
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseException() {
        Map<String, Object> response = new HashMap<>();
        response.put(ErrorMessageConstants.MESSAGE, ErrorMessageConstants.DATABASE_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(ErrorMessageConstants.MESSAGE, ErrorMessageConstants.DATA_FORMAT_ERROR + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException() {
        Map<String, Object> response = new HashMap<>();
        response.put(ErrorMessageConstants.MESSAGE, ErrorMessageConstants.AUTHENTICATION_ERROR);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserOperationException.class)
    public ResponseEntity<Map<String, Object>> handleUserOperationException(UserOperationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(ErrorMessageConstants.MESSAGE, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
