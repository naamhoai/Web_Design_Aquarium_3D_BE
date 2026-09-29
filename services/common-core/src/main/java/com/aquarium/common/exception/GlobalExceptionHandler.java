package com.aquarium.common.exception;

import com.aquarium.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chuẩn hóa mọi lỗi thành {@link ApiResponse}.
 * Nguyên tắc bảo mật: KHÔNG trả nguyên message của exception nội bộ (SQL, class name, stack trace)
 * về client — chỉ ghi log phía server và trả thông điệp chung.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(errorCode.getStatusCode());
        // Cho client biết khi nào được thử lại (chuẩn RFC 9110 cho 429)
        if (errorCode == ErrorCode.ACCOUNT_LOCKED) {
            builder.header("Retry-After", "900");
        } else if (errorCode == ErrorCode.TOO_MANY_REQUESTS) {
            builder.header("Retry-After", "60");
        }
        return builder.body(ApiResponse.error(errorCode.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return validationResponse(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodValidation(HandlerMethodValidationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getAllValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            String message = result.getResolvableErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            errors.put(name != null ? name : "param", message);
        });
        return validationResponse(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(v -> errors.putIfAbsent(lastPathNode(v.getPropertyPath().toString()), v.getMessage()));
        return validationResponse(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadable(HttpMessageNotReadableException exception) {
        String message = "Dữ liệu gửi lên không đúng định dạng JSON";
        if (exception.getCause() instanceof PayloadTooLargeException) {
            return build(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.PAYLOAD_TOO_LARGE.getCode(), ErrorCode.PAYLOAD_TOO_LARGE.getMessage());
        }
        if (exception.getCause() instanceof JsonMappingException mappingException && !mappingException.getPath().isEmpty()) {
            String field = mappingException.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            message = "Giá trị không hợp lệ cho trường '" + field + "'";
        }
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(),
                "Tham số '" + exception.getName() + "' không hợp lệ");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(MissingServletRequestParameterException exception) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(),
                "Thiếu tham số bắt buộc '" + exception.getParameterName() + "'");
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MissingRequestCookieException.class})
    public ResponseEntity<ApiResponse<Object>> handleMissingHeader(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getCode(), ErrorCode.METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaType(HttpMediaTypeNotSupportedException exception) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode(), ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException exception) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getCode(), ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    public ResponseEntity<ApiResponse<Object>> handlePayloadTooLarge(PayloadTooLargeException exception) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.PAYLOAD_TOO_LARGE.getCode(), ErrorCode.PAYLOAD_TOO_LARGE.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException exception) {
        // Người chưa đăng nhập bị @PreAuthorize chặn => 401 (cần đăng nhập), đã đăng nhập => 403 (không đủ quyền)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED.getCode(), ErrorCode.UNAUTHENTICATED.getMessage());
        }
        return build(HttpStatus.FORBIDDEN, ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException exception) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED.getCode(), ErrorCode.UNAUTHENTICATED.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT.getCode(), ErrorCode.CONFLICT.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String reason = exception.getReason() != null ? exception.getReason() : ErrorCode.BAD_REQUEST.getMessage();
        return build(status, status.value(), reason);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception exception) {
        // Ghi đầy đủ ở log server, nhưng không lộ chi tiết nội bộ ra client
        log.error("Unhandled exception", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                "Lỗi hệ thống, vui lòng thử lại sau");
    }

    private static ResponseEntity<ApiResponse<Map<String, String>>> validationResponse(Map<String, String> errors) {
        ApiResponse<Map<String, String>> apiResponse = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .code(400)
                .message("Dữ liệu đầu vào không hợp lệ")
                .data(errors)
                .build();
        return ResponseEntity.badRequest().body(apiResponse);
    }

    private static ResponseEntity<ApiResponse<Object>> build(HttpStatusCode status, int code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }

    private static String lastPathNode(String path) {
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }
}
