package com.product.RealestateTokenization.service;

import com.azure.security.keyvault.secrets.SecretClient;
import com.product.RealestateTokenization.Elasticsearch.PropertyMapper;
import com.product.RealestateTokenization.Elasticsearch.PropertySearch;
import com.product.RealestateTokenization.Elasticsearch.PropertySearchRepository;
import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;



@Service
@Transactional
public class AdminActionService {
    private final KeyVaultService  keyVaultService;

    private final TransactionRepo txRepo;
    private final WalletRepo walletRepo;
    private final PropertyRepo propertyRepo;
    private final TokenOwnershipRepo ownershipRepo;
    private final XrplService xrplService;
    private final UserRepo userRepo;
    private final WalletService walletService;
    private final EnquiryRepo enquiryRepo;

    @Autowired
    private PropertySearchRepository searchRepository;

    public AdminActionService(KeyVaultService keyVaultService, TransactionRepo txRepo, WalletRepo walletRepo, PropertyRepo propertyRepo,
                              TokenOwnershipRepo ownershipRepo, XrplService xrplService, UserRepo userRepo, WalletService walletService, EnquiryRepo enquiryRepo) {
        this.keyVaultService = keyVaultService;
        this.txRepo = txRepo;
        this.walletRepo = walletRepo;
        this.propertyRepo = propertyRepo;
        this.ownershipRepo = ownershipRepo;
        this.xrplService = xrplService;
        this.userRepo = userRepo;
        this.walletService = walletService;
        this.enquiryRepo = enquiryRepo;
    }

    @Value("${xrpl.cold-wallet-address}")
    private String coldWalletAddress;


    @Transactional
    public Transaction confirm(Long txId, Long adminId) throws Exception {

        // ---- VALIDATE DB INTEGRITY ----
        Transaction tx = txRepo.findByIdForUpdate(txId)
                .orElseThrow(() -> new RuntimeException("❌ Transaction not found: " + txId));

        User investor = userRepo.findById(tx.getUserId())
                .orElseThrow(() -> new RuntimeException("❌ Investor not found: " + tx.getUserId()));

        Property property = propertyRepo.findByIdForUpdate(tx.getPropertyId())
                .orElseThrow(() -> new RuntimeException("❌ Property not found: " + tx.getPropertyId()));

        String token = property.getTokenSymbol();
        System.out.println("🚀 Token being transferred = " + token);

        // ---- ENSURE WALLET EXISTS ----
        Wallet wallet = walletRepo.findByUserId(investor.getId()).orElse(null);

        if (wallet == null) {
            wallet = walletService.ensureWallet(investor, tx);
            walletRepo.save(wallet);     // <-- REQUIRED
        }


        // ---- ENSURE XRP RESERVE ----
        if ("CUSTODIAL".equals(investor.getCustodyMode()) && !wallet.isFundedXrp()) {
            xrplService.fundWallet(wallet.getAddress());
            wallet.setFundedXrp(true);
            walletRepo.save(wallet);
        }


        //real funding of xrp
        // ---- ENSURE XRP RESERVE ----
//        if ("CUSTODIAL".equals(investor.getCustodyMode()) && !wallet.isFundedXrp()) {
//
//            // Call Node.js API to activate wallet with 1 XRP
//            RestTemplate rest = new RestTemplate();
//            rest.postForObject(
//                    "http://localhost:3001/activate-wallet",
//                    Map.of("investorAddress", wallet.getAddress()),
//                    String.class
//            );
//            // Mark funded
//            wallet.setFundedXrp(true);
//            walletRepo.save(wallet);
//        }


        // ---- GET INVESTOR SEED FROM AZURE KEY VAULT ----
        String rawSecret = keyVaultService.getSeed(
                "investor-seed-" + investor.getId()
        );


        if (rawSecret == null || rawSecret.isBlank()) {
            throw new RuntimeException("❌ Investor seed not found in Azure Key Vault");
        }

// ---- CHECK TRUSTLINE ----
        boolean hasTL = xrplService.investorHasTrustline(
                wallet.getAddress(),
                xrplService.getIssuerAddressString(),
                token
        );

        if (!hasTL) {
            String tl = xrplService.createInvestorTrustLine(
                    investor.getId(),
                    xrplService.getIssuerAddressString(),
                    token,
                    "10000000000"
            );

            if (!tl.contains("Hash")) {
                throw new RuntimeException("❌ Investor Trustline failed: " + tl);
            }
        }

// ---- CLEAR NORIPPLE ----
        String clearResult = xrplService.clearNoRippleOnTrustline(
                rawSecret,
                coldWalletAddress,
                token,
                "10000000000"
        );

        if (!clearResult.contains("OK")) {
            throw new RuntimeException("❌ Clear NoRipple failed: " + clearResult);
        }


        System.out.println("Investor wallet address = " + wallet.getAddress());
        System.out.println("Token = " + token);
        System.out.println("Amount = " + tx.getTokens());

        // ---- TRANSFER ----
        String txHash = xrplService.transferTokens(
                wallet.getAddress(),
                token,
                tx.getTokens()
        );

        // ---- UPDATE DB ----
        property.setAvailableTokens(property.getAvailableTokens() - tx.getTokens());

        TokenOwnership own = ownershipRepo
                .findByUserIdAndPropertyId(investor.getId(), property.getId())
                .orElse(new TokenOwnership());

        if (own.getPropertyId() == null) own.setPropertyId(property.getId());
        if (own.getUserId() == null) own.setUserId(investor.getId());

        own.setTokens(own.getTokens() + tx.getTokens());
        ownershipRepo.save(own);

        tx.setXrplTxHash(txHash);
        tx.setStatus(TransactionStatus.COMPLETED);

        txRepo.save(tx);
        propertyRepo.save(property);

        return tx;
    }

    public List<Property> getallnotapprovedprop()
    {
        return propertyRepo.findByIsApproved(false);
    }

    @Transactional
    public Property updateisapproved(Long propertyId, Boolean approved) {

        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        property.setIsApproved(approved);

        Property saved = propertyRepo.save(property);

        PropertySearch searchDoc = PropertyMapper.toSearch(saved);
        searchRepository.save(searchDoc);

        return saved;
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


    public List<Transaction> getAllTransaction()
    {
        return txRepo.findAll();
    }


    public List<Enquiry> getAllEnquiries() {
        return enquiryRepo.findAll();
    }


}
