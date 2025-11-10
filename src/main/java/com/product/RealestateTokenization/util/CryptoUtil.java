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
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class CryptoUtil {

    @Value("${master.encryption.key}")
    private String masterKey;  // ✅ non-static field

    public String encrypt(String data) {
        try {
            if (masterKey == null || masterKey.length() != 32) {
                throw new RuntimeException("MASTER_ENCRYPTION_KEY must be 32 characters (found: "
                        + (masterKey == null ? "null" : masterKey.length()) + ")");
            }

            SecretKeySpec keySpec = new SecretKeySpec(masterKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            if (masterKey == null || masterKey.length() != 32) {
                throw new RuntimeException("MASTER_ENCRYPTION_KEY must be 32 characters (found: "
                        + (masterKey == null ? "null" : masterKey.length()) + ")");
            }

            SecretKeySpec keySpec = new SecretKeySpec(masterKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
