package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {

    private final RazorpayClient razorpay;
    private final RedisTemplate<String, String> redis;
    private final TransactionRepo txRepo;
    private final PropertyRepo propRepo;
    private final UserRepo userRepo;
    private final WalletRepo walletRepo;

    public PaymentService(RazorpayClient razorpay, RedisTemplate<String, String> redis, TransactionRepo txRepo,
                          PropertyRepo propRepo, UserRepo userRepo, WalletRepo walletRepo) {
        this.razorpay = razorpay;
        this.redis = redis;
        this.txRepo = txRepo;
        this.propRepo = propRepo;
        this.userRepo = userRepo;
        this.walletRepo = walletRepo;
    }

    @Transactional
    public Order createOrder(Long investorId, Long propertyId, int tokens, String idempotencyKey) {
        if (txRepo.existsByIdempotencyKey(idempotencyKey)) {
            throw new RuntimeException("Already processed");
        }

        Property p = propRepo.findByIdForUpdate(propertyId).orElseThrow();
        String key = "reserve:" + propertyId + ":" + investorId;
        long reserved = redis.opsForValue().increment(key, tokens);
        redis.expire(key, 15, TimeUnit.MINUTES);

        if (p.getAvailableTokens() < reserved) {
            redis.opsForValue().decrement(key, tokens);
            throw new RuntimeException("Not enough tokens");
        }

        BigDecimal amount = BigDecimal.valueOf(p.getTokenPrice()).multiply(BigDecimal.valueOf(tokens));
        User user = userRepo.findById(investorId).orElseThrow();
        if ("CUSTODIAL".equals(user.getCustodyMode()) && !walletRepo.existsByUserId(investorId)) {
            amount = amount.add(BigDecimal.valueOf(800));
        }

        JSONObject orderReq = new JSONObject()
                .put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue())
                .put("currency", "INR")
                .put("receipt", idempotencyKey);

        Order razorOrder;
        try {
            razorOrder = razorpay.Orders.create(orderReq);
        } catch (Exception e) {
            redis.opsForValue().decrement(key, tokens);
            throw new RuntimeException(e);
        }

        Transaction tx = new Transaction();
        tx.setUserId(investorId);
        tx.setPropertyId(propertyId);
        tx.setTokens(tokens);
        tx.setAmount(amount.doubleValue());
        tx.setRazorpayOrderId(razorOrder.get("id").toString());
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(TransactionStatus.PENDING_PAYMENT);
        txRepo.save(tx);

        return razorOrder;
    }
}
