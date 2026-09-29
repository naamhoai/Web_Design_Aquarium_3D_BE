package com.aquarium.common.exception;

import java.io.IOException;

/**
 * Ném ra khi body của request vượt quá giới hạn cấu hình (chống DoS bằng payload lớn).
 * Kế thừa IOException để có thể ném từ bên trong ServletInputStream.
 */
public class PayloadTooLargeException extends IOException {

    public PayloadTooLargeException(long limitBytes) {
        super("Request body exceeds limit of " + limitBytes + " bytes");
    }
}
