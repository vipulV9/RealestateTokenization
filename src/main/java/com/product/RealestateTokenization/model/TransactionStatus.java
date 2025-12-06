package com.product.RealestateTokenization.model;

public enum TransactionStatus {
    PENDING_PAYMENT,
    PAYMENT_RECEIVED_PENDING_ADMIN,
    COMPLETED,
    REFUNDED,
    FAILED,
    RENT_DISTRIBUTION,
    PENDING_INTEREST,           // New
    DOCS_SENT,
}
