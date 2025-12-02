package com.riverflow.exception;

import lombok.Getter;

/**
 * Đại diện lỗi khi gọi AI upstream (OpenAI, v.v.).
 * Giữ lại status code để controller/handler trả đúng HTTP status cho client.
 */
@Getter
public class AiUpstreamException extends RuntimeException {
    private final int status;

    public AiUpstreamException(int status, String message) {
        super(message);
        this.status = status;
    }
}

