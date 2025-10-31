package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminActionService {

    private final TransactionRepo txRepo;
    private final WalletRepo walletRepo;
    private final PropertyRepo propertyRepo;
    private final TokenOwnershipRepo ownershipRepo;
    private final XrplService xrplService;
    private final UserRepo userRepo;

    public AdminActionService(TransactionRepo txRepo, WalletRepo walletRepo, PropertyRepo propertyRepo,
                              TokenOwnershipRepo ownershipRepo, XrplService xrplService, UserRepo userRepo) {
        this.txRepo = txRepo;
        this.walletRepo = walletRepo;
        this.propertyRepo = propertyRepo;
        this.ownershipRepo = ownershipRepo;
        this.xrplService = xrplService;
        this.userRepo = userRepo;
    }

    public Transaction confirm(Long txId, Long adminId) throws Exception {
        Transaction tx = txRepo.findByIdForUpdate(txId).orElseThrow();
        if (tx.getStatus() == TransactionStatus.COMPLETED) return tx;

        User investor = userRepo.findById(tx.getUserId()).orElseThrow();
        Property property = propertyRepo.findByIdForUpdate(tx.getPropertyId()).orElseThrow();
        Wallet wallet = walletRepo.findByUserId(investor.getId()).orElseThrow();

        if ("CUSTODIAL".equals(investor.getCustodyMode()) && !wallet.isFundedXrp()) {
            String fundTx = xrplService.fundWallet(wallet.getAddress());
            wallet.setFundedXrp(true);
            walletRepo.save(wallet);
        }

        xrplService.createTrustline(wallet.getAddress(), property.getTokenSymbol());
        String transferTx = xrplService.transferTokens(wallet.getAddress(), property.getTokenSymbol(), tx.getTokens());

        property.setAvailableTokens(property.getAvailableTokens() - tx.getTokens());
        TokenOwnership own = ownershipRepo.findByUserIdAndPropertyId(investor.getId(), property.getId())
                .orElse(new TokenOwnership());
        own.setTokens(own.getTokens() + tx.getTokens());
        ownershipRepo.save(own);

        tx.setXrplTxHash(transferTx);
        tx.setStatus(TransactionStatus.COMPLETED);
        txRepo.save(tx);
        return tx;
    }

    public Transaction reject(Long id) {
        Transaction tx = txRepo.findById(id).orElseThrow();
        tx.setStatus(TransactionStatus.REFUNDED);
        txRepo.save(tx);
        return tx;
    }

    public java.util.List<Transaction> getPendingPayments() {
        return txRepo.findByStatus(String.valueOf(TransactionStatus.PAYMENT_RECEIVED_PENDING_ADMIN));
    }
}