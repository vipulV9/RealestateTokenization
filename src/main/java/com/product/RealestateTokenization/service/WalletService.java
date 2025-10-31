package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.model.Transaction;
import com.product.RealestateTokenization.model.User;
import com.product.RealestateTokenization.model.Wallet;
import com.product.RealestateTokenization.repository.WalletRepo;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final WalletRepo repo;
    private final XrplService xrpl;
    private final UserService userService;

    public WalletService(WalletRepo repo, XrplService xrpl, UserService userService) {
        this.repo = repo;
        this.xrpl = xrpl;
        this.userService = userService;
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
}