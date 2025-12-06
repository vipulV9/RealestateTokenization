// src/main/java/com/product/RealestateTokenization/service/KycSessionService.java

package com.product.RealestateTokenization.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;                    // ← ADD THIS
import java.util.concurrent.TimeUnit;

@Service
public class KycSessionService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "kyc:session:";
    private static final long EXPIRY = 10; // minutes

    public String createSession(String pan, String fullPhone) {
        String sessionId = java.util.UUID.randomUUID().toString();
        KycSessionData data = new KycSessionData(pan.toUpperCase(), fullPhone);
        redisTemplate.opsForValue().set(PREFIX + sessionId, data, EXPIRY, TimeUnit.MINUTES);
        return sessionId;
    }

    public KycSessionData getAndDelete(String sessionId) {
        String key = PREFIX + sessionId;
        KycSessionData data = (KycSessionData) redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return data;
    }

    // ← ADD "implements Serializable" HERE
    public static record KycSessionData(String pan, String phone) implements Serializable {
        private static final long serialVersionUID = 1L;  // ← ADD THIS LINE
    }
}