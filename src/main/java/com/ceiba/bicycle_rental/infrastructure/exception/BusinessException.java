package com.ceiba.bicycle_rental.infrastructure.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
