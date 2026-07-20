package com.mes.backend.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final String status;

    public CustomException(String status, String message) {
        super(message);
        this.status = status;
    }
}
