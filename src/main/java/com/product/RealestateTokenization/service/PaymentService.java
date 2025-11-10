package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
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
        this.txRepo = txRepo;this.propRepo = propRepo;
       this.userRepo = userRepo;
        this.walletRepo = walletRepo;
   }
//
//    @Transactional
//    public Order createOrder(Long investorId, Long propertyId, int tokens, String idempotencyKey) {
//        if (txRepo.existsByIdempotencyKey(idempotencyKey)) {
//            throw new RuntimeException("Already processed");
//        }
//
//        Property p = propRepo.findByIdForUpdate(propertyId).orElseThrow();
//        String key = "reserve:" + propertyId + ":" + investorId;
//        long reserved = redis.opsForValue().increment(key, tokens);
//        redis.expire(key, 15, TimeUnit.MINUTES);
//
//        if (p.getAvailableTokens() < reserved) {
//            redis.opsForValue().decrement(key, tokens);
//            throw new RuntimeException("Not enough tokens");
//        }
//
//        BigDecimal amount = BigDecimal.valueOf(p.getTokenPrice()).multiply(BigDecimal.valueOf(tokens));
//        User user = userRepo.findById(investorId).orElseThrow();
//        if ("CUSTODIAL".equals(user.getCustodyMode()) && !walletRepo.existsByUserId(investorId)) {
//            amount = amount.add(BigDecimal.valueOf(800));
//        }
//
//        JSONObject orderReq = new JSONObject()
//                .put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue())
//                .put("currency", "INR")
//                .put("receipt", idempotencyKey);
//
//        Order razorOrder;
//        try {
//            razorOrder = razorpay.Orders.create(orderReq);
//        } catch (Exception e) {
//            redis.opsForValue().decrement(key, tokens);
//            throw new RuntimeException(e);
//        }
//
//        Transaction tx = new Transaction();
//        tx.setUserId(investorId);
//        tx.setPropertyId(propertyId);
//        tx.setTokens(tokens);
//        tx.setAmount(amount.doubleValue());
//        tx.setRazorpayOrderId(razorOrder.get("id").toString());
//        tx.setIdempotencyKey(idempotencyKey);
//        tx.setStatus(TransactionStatus.PENDING_PAYMENT);
//        txRepo.save(tx);
//
//        return razorOrder;
//    }
//@Transactional
//public Order createOrder(Long investorId, Long propertyId, int tokens, String idempotencyKey) {
//    if (txRepo.existsByIdempotencyKey(idempotencyKey))
//        throw new RuntimeException("Duplicate");
//
//    Property p = propRepo.findByIdForUpdate(propertyId).orElseThrow();
//    String rKey = "reserve:" + propertyId + ":" + investorId;
//    long reserved = redis.opsForValue().increment(rKey, tokens);
//    redis.expire(rKey, 15, TimeUnit.MINUTES);
//    if (p.getAvailableTokens() < reserved) {
//        redis.opsForValue().decrement(rKey, tokens);
//        throw new RuntimeException("Not enough tokens");
//    }
//
//    BigDecimal base = BigDecimal.valueOf(p.getTokenPrice()).multiply(BigDecimal.valueOf(tokens));
//    BigDecimal total = base;
//
//    User u = userRepo.findById(investorId).orElseThrow();
//    boolean hasWallet = walletRepo.existsByUserId(investorId);
//    boolean needFee = "CUSTODIAL".equals(u.getCustodyMode()) && !hasWallet;
//    if (needFee) total = total.add(BigDecimal.valueOf(800));
//
//    JSONObject jo = new JSONObject()
//            .put("amount", total.multiply(BigDecimal.valueOf(100)).longValue())
//            .put("currency", "INR")
//            .put("receipt", idempotencyKey);
//
//    Order razorOrder;
//    try { razorOrder = razorpay.Orders.create(jo); }
//    catch (Exception e) {
//        redis.opsForValue().decrement(rKey, tokens);
//        throw new RuntimeException(e);
//    }
//
//    Transaction tx = new Transaction();
//    tx.setUserId(investorId);
//    tx.setPropertyId(propertyId);
//    tx.setTokens(tokens);
//    tx.setAmount(total.doubleValue());
//    tx.setRazorpayOrderId(razorOrder.get("id").toString());
//    tx.setIdempotencyKey(idempotencyKey);
//    tx.setStatus(TransactionStatus.PENDING_PAYMENT);
//    tx.setWalletCreationFee(needFee);
//    tx.setWalletFeeAmount(needFee ? 800.0 : 0.0);
//    txRepo.save(tx);
//
//    return razorOrder;
//}

    @Transactional
    public Map<String, Object> createOrder(Long investorId, Long propertyId, int tokens, String idempotencyKey) {
        // ✅ Prevent duplicate requests
        if (txRepo.existsByIdempotencyKey(idempotencyKey))
            throw new RuntimeException("Duplicate transaction request");

        // ✅ Lock property for update (prevent overselling)
        Property p = propRepo.findByIdForUpdate(propertyId).orElseThrow();
        String rKey = "reserve:" + propertyId + ":" + investorId;

        long reserved = redis.opsForValue().increment(rKey, tokens);
        redis.expire(rKey, 15, TimeUnit.MINUTES);

        if (p.getAvailableTokens() < reserved) {
            redis.opsForValue().decrement(rKey, tokens);
            throw new RuntimeException("Not enough tokens available");
        }

        // ✅ Token price calculation
        BigDecimal base = BigDecimal.valueOf(p.getTokenPrice()).multiply(BigDecimal.valueOf(tokens));
        BigDecimal total = base;

        // ✅ Wallet + custody mode checks
        User u = userRepo.findById(investorId).orElseThrow();
        boolean hasWallet = walletRepo.existsByUserId(investorId);
        boolean needFee = "CUSTODIAL".equals(u.getCustodyMode()) && !hasWallet;

        if (needFee) {
            total = total.add(BigDecimal.valueOf(800));
        }

        // ✅ Prepare Razorpay order creation
        JSONObject jo = new JSONObject()
                .put("amount", total.multiply(BigDecimal.valueOf(100)).longValue()) // Convert INR → paise
                .put("currency", "INR")
                .put("receipt", idempotencyKey);

        Order razorOrder;
        try {
            razorOrder = razorpay.Orders.create(jo);
        } catch (Exception e) {
            redis.opsForValue().decrement(rKey, tokens);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }

        // ✅ Save transaction in DB
        Transaction tx = new Transaction();
        tx.setUserId(investorId);
        tx.setPropertyId(propertyId);
        tx.setTokens(tokens);
        tx.setAmount(total.doubleValue());
        tx.setRazorpayOrderId(razorOrder.get("id").toString());
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(TransactionStatus.PENDING_PAYMENT);
        tx.setWalletCreationFee(needFee);
        tx.setWalletFeeAmount(needFee ? 800.0 : 0.0);
        txRepo.save(tx);

        // ✅ Return safe JSON response instead of Razorpay object
        Map<String, Object> response = new HashMap<>();
        response.put("id", razorOrder.get("id"));
        response.put("amount", razorOrder.get("amount"));
        response.put("currency", razorOrder.get("currency"));
        response.put("status", razorOrder.get("status"));
        response.put("receipt", razorOrder.get("receipt"));
        response.put("tokens", tokens);
        response.put("totalPayable", total);
        response.put("walletFeeIncluded", needFee);

        return response;
    }



    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public String handleWebhook(String payload, String signatureHeader) {
        try {
            // ✅ Verify Razorpay signature
            boolean isValid = Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
            if (!isValid) {
                return "Invalid Signature";
            }

            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");

            if (eventType.equals("payment.captured")) {
                JSONObject payment = event.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String orderId = payment.getString("order_id");
                String paymentId = payment.getString("id");
                double amount = payment.getDouble("amount") / 100.0;

                // ✅ Fetch transaction safely using Optional
                Transaction txn = txRepo.findByRazorpayOrderId(orderId).orElse(null);

                if (txn != null) {
                    txn.setRazorpayPaymentId(paymentId);
                    txn.setStatus(TransactionStatus.PAYMENT_RECEIVED_PENDING_ADMIN);
                    txn.setAmount(amount);
                    txRepo.save(txn);
                    return "✅ Payment Captured and Updated";
                } else {
                    return "⚠️ Transaction not found for order_id: " + orderId;
                }

            } else if (eventType.equals("payment.failed")) {
                JSONObject payment = event.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String orderId = payment.getString("order_id");

                Transaction txn = txRepo.findByRazorpayOrderId(orderId).orElse(null);

                if (txn != null) {
                    txn.setStatus(TransactionStatus.FAILED);
                    txRepo.save(txn);
                }
                return "❌ Payment Failed Updated";
            }

            return "Unhandled Event: " + eventType;

        } catch (Exception e) {
            e.printStackTrace();
            return "Webhook Error: " + e.getMessage();
        }
    }

}
