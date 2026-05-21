package com.sigeclin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlergiaActivaException extends RuntimeException {
    public AlergiaActivaException(String message) {
        super(message);
    }
}
