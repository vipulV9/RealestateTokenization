package com.product.RealestateTokenization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.RealestateTokenization.model.Transaction;
import com.product.RealestateTokenization.model.User;
import com.product.RealestateTokenization.model.Wallet;
import com.product.RealestateTokenization.repository.WalletRepo;
import com.product.RealestateTokenization.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.crypto.keys.Base58EncodedSecret;
import org.xrpl.xrpl4j.crypto.keys.KeyPair;
import org.xrpl.xrpl4j.crypto.keys.PrivateKeyable;
import org.xrpl.xrpl4j.crypto.keys.Seed;
import org.xrpl.xrpl4j.crypto.signing.SignatureService;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.crypto.signing.bc.BcSignatureService;
import org.xrpl.xrpl4j.model.jackson.ObjectMapperFactory;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.TrustSet;

import java.util.List;

@Service
public class WalletService {

    @Autowired
    CryptoUtil cryptoUtil;

    private final WalletRepo repo;
    private final XrplService xrpl;
    private final UserService userService;

    public WalletService(WalletRepo repo, XrplService xrpl, UserService userService) {
        this.repo = repo;
        this.xrpl = xrpl;
        this.userService = userService;
    }
    public boolean existsByUserId(Long userId) {
        return repo.existsByUserId(userId);
    }
    public Wallet ensureWallet(User user, Transaction tx) {
        return repo.findByUserId(user.getId())
                .orElseGet(() -> createCustodialWallet(user, tx));
    }

    private Wallet createCustodialWallet(User user, Transaction tx) {
        if (!"CUSTODIAL".equals(user.getCustodyMode())) {
            throw new IllegalStateException("Custody mode not CUSTODIAL");
        }

        WalletInfo info = xrpl.createWallet();
        Wallet w = new Wallet();
        w.setUser(user);
        w.setAddress(info.address());
        w.setSecretEncrypted(info.encryptedSecret());
        w.setFundedXrp(false);
        repo.save(w);

        tx.setWalletCreationFee(true);
        tx.setWalletFeeAmount(800.0);
        return w;
    }

    public Wallet getWallet(Long userId) {
        return repo.findByUserId(userId).orElseThrow();
    }
    public List<Wallet> getAllWallet()
    {
        return  repo.findAll();
    }



    public void setInvestorAddress(Long userId, String address) {
        User user = userService.getUser(userId);
        if ("CUSTODIAL".equals(user.getCustodyMode())) {
            throw new IllegalStateException("Custodial users cannot set address");
        }
        Wallet w = new Wallet();
        w.setUser(user);
        w.setAddress(address);
        w.setSecretEncrypted(null);
        repo.save(w);
    }

    public void requestSell(Long aLong, Long id, int tokens) {
    }
    public String signWithInvestorSecret(Wallet wallet, String unsignedJson) {
        try {
            String rawSecret = wallet.getSecretEncrypted(); // DIRECT USE
            Seed seed = Seed.fromBase58EncodedSecret(Base58EncodedSecret.of(rawSecret));
            KeyPair kp = seed.deriveKeyPair();

            SignatureService ss = new BcSignatureService();
            ObjectMapper mapper = ObjectMapperFactory.create();

            JsonNode node = mapper.readTree(unsignedJson);
            String type = node.get("TransactionType").asText();

            SingleSignedTransaction<?> signed;

            if ("TrustSet".equals(type)) {
                TrustSet trust = mapper.readValue(unsignedJson, TrustSet.class);
                signed = ss.sign(kp.privateKey(), trust);
            } else {
                throw new RuntimeException("Unsupported TransactionType: " + type);
            }

            return signed.signedTransactionBytes().hexValue();

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign investor transaction: " + e.getMessage(), e);
        }
    }






}
