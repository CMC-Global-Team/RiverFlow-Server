package com.riverflow.exception;

import com.riverflow.dto.MessageResponse;
import com.riverflow.exception.mindmap.InvalidMindmapDataException;
import com.riverflow.exception.mindmap.MindmapAccessDeniedException;
import com.riverflow.exception.mindmap.MindmapNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler để xử lý các exception trong ứng dụng
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Xử lý IllegalArgumentException (Bad Request)
     * Ví dụ: Email không tồn tại, dữ liệu không hợp lệ
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý EmailAlreadyExistsException (Conflict)
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<MessageResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        log.error("EmailAlreadyExistsException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý InvalidTokenException (Bad Request)
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<MessageResponse> handleInvalidTokenException(InvalidTokenException ex) {
        log.error("InvalidTokenException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý EmailNotVerifiedException (Forbidden)
     * Khi user cố gắng đăng nhập với email chưa xác thực
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<MessageResponse> handleEmailNotVerifiedException(EmailNotVerifiedException ex) {
        log.error("EmailNotVerifiedException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý BadCredentialsException (Unauthorized)
     * Spring Security throw khi đăng nhập sai email/password
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("BadCredentialsException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý Validation Exception (từ @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        response.put("message", "Dữ liệu không hợp lệ");
        response.put("errors", errors);
        
        log.error("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Xử lý MindmapNotFoundException (Not Found)
     */
    @ExceptionHandler(MindmapNotFoundException.class)
    public ResponseEntity<MessageResponse> handleMindmapNotFoundException(MindmapNotFoundException ex) {
        log.error("MindmapNotFoundException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý MindmapAccessDeniedException (Forbidden)
     */
    @ExceptionHandler(MindmapAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleMindmapAccessDeniedException(MindmapAccessDeniedException ex) {
        log.error("MindmapAccessDeniedException: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        // Include shareToken if available (for public mindmaps)
        if (ex.getShareToken() != null) {
            response.put("shareToken", ex.getShareToken());
        }
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * Xử lý InvalidMindmapDataException (Bad Request)
     */
    @ExceptionHandler(InvalidMindmapDataException.class)
    public ResponseEntity<MessageResponse> handleInvalidMindmapDataException(InvalidMindmapDataException ex) {
        log.error("InvalidMindmapDataException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý lỗi từ AI Upstream (giữ nguyên status như 429, 401, 503...)
     */
    @ExceptionHandler(AiUpstreamException.class)
    public ResponseEntity<MessageResponse> handleAiUpstream(AiUpstreamException ex) {
        log.error("AiUpstreamException: status={} message={}", ex.getStatus(), ex.getMessage());
        HttpStatus status;
        try {
            status = HttpStatus.valueOf(ex.getStatus());
        } catch (Exception ignore) {
            status = HttpStatus.BAD_GATEWAY; // fallback
        }
        return ResponseEntity.status(status).body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Xử lý các exception chung (Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<MessageResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("Không tìm thấy tài nguyên hoặc endpoint"));
    }

    /**
     * ClientAbortException xảy ra khi client đóng kết nối trước khi server ghi xong response.
     * Trường hợp này không phải lỗi ứng dụng, chỉ ghi log cảnh báo và trả No Content.
     */
    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Void> handleClientAbort(ClientAbortException ex) {
        log.warn("Client aborted connection: {}", ex.getMessage());
        return ResponseEntity.noContent().build();
    }
}

