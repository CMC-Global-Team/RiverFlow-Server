package com.riverflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Tự động trả về lỗi 400 (Bad Request) khi exception này được ném ra
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}