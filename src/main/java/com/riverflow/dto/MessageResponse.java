package com.riverflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Tạo constructor: new MessageResponse("Some message")
public class MessageResponse {
    private String message;
}