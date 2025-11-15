package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminActionService {

    private final TransactionRepo txRepo;
    private final WalletRepo walletRepo;
    private final PropertyRepo propertyRepo;
    private final TokenOwnershipRepo ownershipRepo;
    private final XrplService xrplService;
    private final UserRepo userRepo;
    private final WalletService walletService;

    public AdminActionService(TransactionRepo txRepo, WalletRepo walletRepo, PropertyRepo propertyRepo,
                              TokenOwnershipRepo ownershipRepo, XrplService xrplService, UserRepo userRepo, WalletService walletService) {
        this.txRepo = txRepo;
        this.walletRepo = walletRepo;
        this.propertyRepo = propertyRepo;
        this.ownershipRepo = ownershipRepo;
        this.xrplService = xrplService;
        this.userRepo = userRepo;
        this.walletService = walletService;
    }

    @Transactional
    public Transaction confirm(Long txId, Long adminId) throws Exception {

        Transaction tx = txRepo.findByIdForUpdate(txId).orElseThrow();
        if (tx.getStatus() == TransactionStatus.COMPLETED) return tx;

        User investor = userRepo.findById(tx.getUserId()).orElseThrow();
        Property property = propertyRepo.findByIdForUpdate(tx.getPropertyId()).orElseThrow();

        // ---- WALLET ----
        Wallet wallet = walletRepo.findByUserId(investor.getId())
                .orElseGet(() -> walletService.ensureWallet(investor, tx));

        // ---- FUND XRP (testnet) ----
        if ("CUSTODIAL".equals(investor.getCustodyMode()) && !wallet.isFundedXrp()) {
            xrplService.fundWallet(wallet.getAddress());
            wallet.setFundedXrp(true);
            walletRepo.save(wallet);
        }

        // ✅ NEW: Ensure HOT wallet has trustline to COLD wallet (issuer)
        String hotTl = xrplService.createHotWalletTrustLine(property.getTokenSymbol());
        System.out.println("Hot wallet trustline = " + hotTl);


        // ✅ Investor must trust HOT WALLET (the sender)
        String investorTl = xrplService.createInvestorTrustLine(
                wallet.getSecretEncrypted(),
                xrplService.getHotWalletAddress(),
                property.getTokenSymbol(),
                "10000000000"
        );

        System.out.println("Investor trustline = " + investorTl);

        if (!investorTl.contains("Hash")) {
            throw new RuntimeException("Investor trustline failed: " + investorTl);
        }

        // ---- TRANSFER TOKENS ----
        String txHash = xrplService.transferTokensToInvestor(
                wallet.getAddress(),
                property.getTokenSymbol(),
                tx.getTokens()
        );

        System.out.println("Token transfer hash = " + txHash);

        // ---- UPDATE OWNERSHIP ----
        property.setAvailableTokens(property.getAvailableTokens() - tx.getTokens());

        TokenOwnership own = ownershipRepo.findByUserIdAndPropertyId(investor.getId(), property.getId())
                .orElse(new TokenOwnership());

        if (own.getPropertyId() == null) own.setPropertyId(property.getId());
        if (own.getUserId() == null) own.setUserId(investor.getId());

        own.setTokens(own.getTokens() + tx.getTokens());
        ownershipRepo.save(own);

        // ---- UPDATE TRANSACTION ----
        tx.setXrplTxHash(txHash);
        tx.setStatus(TransactionStatus.COMPLETED);
        txRepo.save(tx);
        propertyRepo.save(property);

        return tx;
    }


    public Transaction reject(Long id) {
        Transaction tx = txRepo.findById(id).orElseThrow();
        tx.setStatus(TransactionStatus.REFUNDED);
        txRepo.save(tx);
        return tx;
    }

//    public java.util.List<Transaction> getPendingPayments() {
//        return txRepo.findByStatus(String.valueOf(TransactionStatus.PAYMENT_RECEIVED_PENDING_ADMIN));
//    }

    public List<Transaction> getPendingPayments() {
        return txRepo.findByStatus(TransactionStatus.PAYMENT_RECEIVED_PENDING_ADMIN);
    }



}
