package com.product.RealestateTokenization.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtil {
    private static final String KEY = System.getenv("MASTER_ENCRYPTION_KEY");

    public static String encrypt(String data) {
        try {
            if (KEY == null || KEY.length() != 32) {
                throw new RuntimeException("MASTER_ENCRYPTION_KEY must be 32 characters");
            }
            SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
}