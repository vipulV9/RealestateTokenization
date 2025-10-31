package com.product.RealestateTokenization.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class User {
    @Id @GeneratedValue private Long id;
    private String name, email, phone, passwordHash, role = "INVESTOR", custodyMode = "NON_CUSTODIAL";
    private String kycStatus = "PENDING";
    public void setKycStatus(String status) { this.kycStatus = status; }
}
/*
# ==============================
# DATABASE
# ==============================
MYSQL_USER=root
MYSQL_PASSWORD=secret

# ==============================
# RAZORPAY (TEST MODE)
# ==============================
RAZORPAY_KEY_ID=rzp_test_YourKey123
RAZORPAY_KEY_SECRET=YourTestSecret456
RAZORPAY_WEBHOOK_SECRET=YourWebhook789

# ==============================
# XRPL (TESTNET)
# ==============================
HOT_WALLET_SEED=sYourHotTestSeed123...
COLD_WALLET_SEED=sYourColdTestSeed456...
ISSUER_ADDRESS=rYourHotAddress123...

# ==============================
# ENCRYPTION (32-CHAR KEY)
# ==============================
MASTER_ENCRYPTION_KEY=32CharSecretKeyHere12345678901234

# ==============================
# CUSTODY MODE
# ==============================
CUSTODY_MODE=CUSTODIAL */