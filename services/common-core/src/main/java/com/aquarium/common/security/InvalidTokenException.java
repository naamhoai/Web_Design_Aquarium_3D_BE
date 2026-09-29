package com.aquarium.common.security;

/** Token không hợp lệ (sai chữ ký, hết hạn, sai loại, sai issuer/audience...). */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
