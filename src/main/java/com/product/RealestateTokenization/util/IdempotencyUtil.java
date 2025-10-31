package com.product.RealestateTokenization.util;


import java.util.UUID;

public class IdempotencyUtil {
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}