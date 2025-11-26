package com.product.RealestateTokenization.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long userId, propertyId;
    private int tokens;
    private double amount;
    private String razorpayOrderId, razorpayPaymentId, xrplTxHash, idempotencyKey;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING_PAYMENT;

    private boolean walletCreationFee = false;
    private double walletFeeAmount = 0.0;

    public void setXrplTxHash(String hash) { this.xrplTxHash = hash; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public void setWalletCreationFee(boolean flag) { this.walletCreationFee = flag; }
    public void setWalletFeeAmount(double amount) { this.walletFeeAmount = amount; }
}
