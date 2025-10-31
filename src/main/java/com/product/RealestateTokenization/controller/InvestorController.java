package com.product.RealestateTokenization.controller;


import com.product.RealestateTokenization.model.Property;
import com.product.RealestateTokenization.model.Wallet;
import com.product.RealestateTokenization.service.PaymentService;
import com.product.RealestateTokenization.service.PropertyService;
import com.product.RealestateTokenization.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/investor")
public class InvestorController {

    private final PropertyService propertyService;
    private final WalletService walletService;
    private final PaymentService paymentService;

    public InvestorController(PropertyService propertyService, WalletService walletService, PaymentService paymentService) {
        this.propertyService = propertyService;
        this.walletService = walletService;
        this.paymentService = paymentService;
    }

    @GetMapping("/properties")
    public List<Property> listProperties() {
        return propertyService.getOnChainProperties();
    }

    @GetMapping("/properties/{id}")
    public Property getProperty(@PathVariable Long id) {
        return propertyService.getProperty(id);
    }

    @PostMapping("/payment/create-order")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest req) {
        var order = paymentService.createOrder(req.investorId(), req.propertyId(), req.tokens(), req.idempotencyKey());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/wallet")
    public Wallet getWallet(@RequestHeader("userId") Long userId) {
        return walletService.getWallet(userId);
    }

    @PostMapping("/properties/{id}/sell")
    public ResponseEntity<?> sell(@PathVariable Long id, @RequestBody SellRequest req) {
        walletService.requestSell(req.userId(), id, req.tokens());
        return ResponseEntity.ok("Sell request created");
    }

    @PostMapping("/wallet/address")
    public ResponseEntity<?> setAddress(@RequestBody AddressRequest req) {
        walletService.setInvestorAddress(req.userId(), req.address());
        return ResponseEntity.ok("Address saved");
    }
}

record OrderRequest(Long investorId, Long propertyId, int tokens, String idempotencyKey) {}
record SellRequest(Long userId, int tokens) {}
record AddressRequest(Long userId, String address) {}