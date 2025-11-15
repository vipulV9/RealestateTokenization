//package com.product.RealestateTokenization.util;
//
//import javax.crypto.Cipher;
//import javax.crypto.spec.SecretKeySpec;
//import java.util.Base64;
//
//public class CryptoUtil {
//    private static final String KEY = System.getenv("MASTER_ENCRYPTION_KEY");
//
//    public static String encrypt(String data) {
//        try {
//            if (KEY == null || KEY.length() != 32) {
//                throw new RuntimeException("MASTER_ENCRYPTION_KEY must be 32 characters");
//            }
//            SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), "AES");
//            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//            cipher.init(Cipher.ENCRYPT_MODE, key);
//            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
//        } catch (Exception e) {
//            throw new RuntimeException("Encryption failed", e);
//        }
//    }
//}
package com.product.RealestateTokenization.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CryptoUtil {

    @Value("${master.encryption.key}")
    private String masterKey;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public String encrypt(String data) {
        try {
            if (masterKey == null || masterKey.length() != 32) {
                throw new RuntimeException("MASTER_ENCRYPTION_KEY must be 32 chars. Found: " +
                        (masterKey == null ? "null" : masterKey.length()));
            }

            byte[] keyBytes = masterKey.getBytes("UTF-8");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));

            // Prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            if (masterKey == null || masterKey.length() != 32) {
                throw new RuntimeException("MASTER_ENCRYPTION_KEY must be 32 chars. Found: " +
                        (masterKey == null ? "null" : masterKey.length()));
            }

            byte[] keyBytes = masterKey.getBytes("UTF-8");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, "UTF-8");

        } catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }
}
