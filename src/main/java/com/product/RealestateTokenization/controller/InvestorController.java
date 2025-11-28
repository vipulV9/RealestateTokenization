package com.product.RealestateTokenization.controller;


import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.TransactionRepo;
import com.product.RealestateTokenization.service.PaymentService;
import com.product.RealestateTokenization.service.PropertyService;
import com.product.RealestateTokenization.service.UserService;
import com.product.RealestateTokenization.service.WalletService;
import com.razorpay.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/investor")
public class InvestorController {

    private final PropertyService propertyService;
    private final WalletService walletService;
    private final PaymentService paymentService;

    @Autowired
    UserService userService;

    @Autowired
    TransactionRepo transactionRepo;

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

//    @PostMapping("/payment/create-order")
//    public ResponseEntity<?> createOrder(@RequestBody OrderRequest req) {
//        var order = paymentService.createOrder(req.investorId(), req.propertyId(), req.tokens(), req.idempotencyKey());
//        return ResponseEntity.ok(order);
//    }

//    @PostMapping("/payment/create-order")
//    public ResponseEntity<?> createOrder(@AuthenticationPrincipal User user,
//                                         @RequestBody OrderRequest req) {
//
//        // 🧩 Temporarily skipping KYC verification for testing
//        // if (!Objects.equals(user.getKycStatus(), "APPROVED"))
//        //     throw new RuntimeException("KYC not approved");
//
//        Order order = paymentService.createOrder(
//                user.getId(), req.propertyId(), req.tokens(), req.idempotencyKey());
//        return ResponseEntity.ok(order);
//    }

//    @PostMapping("/payment/create-order")
//    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
//        try {
//            Long investorId = Long.valueOf(payload.get("investorId").toString());
//            Long propertyId = Long.valueOf(payload.get("propertyId").toString());
//            int tokens = Integer.parseInt(payload.get("tokens").toString());
//            String idempotencyKey = payload.get("idempotencyKey").toString();
//
//            Map<String, Object> order = paymentService.createOrder(investorId, propertyId, tokens, idempotencyKey);
//            return ResponseEntity.ok(order);
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }

    @PostMapping("/payment/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
        try {
            Long investorId = getCurrentUserId();   // secure
            Long propertyId = Long.valueOf(payload.get("propertyId").toString());
            int tokens = Integer.parseInt(payload.get("tokens").toString());
            String idempotencyKey = payload.get("idempotencyKey").toString();

            Map<String, Object> order = paymentService.createOrder(
                    investorId, propertyId, tokens, idempotencyKey
            );

            return ResponseEntity.ok(order);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("X-Razorpay-Signature") String signatureHeader) {
        String result = paymentService.handleWebhook(payload, signatureHeader);
        return ResponseEntity.ok(result);
    }


//
//    @GetMapping("/wallet")
//    public Wallet getWallet(@RequestHeader("userId") Long userId) {
//        return walletService.getWallet(userId);
//    }
@GetMapping("/wallet")
public Wallet getWallet() {
    Long userId = getCurrentUserId();  // SECURE extraction
    return walletService.getWallet(userId);
}


    @PostMapping("/properties/{id}/sell")
    public ResponseEntity<?> sell(@PathVariable Long id, @RequestBody SellRequest req) {
        Long userId = getCurrentUserId();
        walletService.requestSell(userId, id, req.tokens());
        return ResponseEntity.ok("Sell request created");
    }

//    @PostMapping("/wallet/address")
//    public ResponseEntity<?> setAddress(@RequestBody AddressRequest req) {
//        Long userId = getCurrentUserId();
//        walletService.setInvestorAddress(userId, req.address());
//        return ResponseEntity.ok("Address saved");
//    }


    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
// =========== ADD THESE TO InvestorController.java ===========

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        Long userId = getCurrentUserId();
        User user = userService.getUser(userId);

        UserProfileDTO dto = new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getKycStatus(),
                user.getCustodyMode()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/portfolio")
    public ResponseEntity<UserInvestmentDTO> getMyPortfolio() {
        Long userId = getCurrentUserId();

        // Reuse the same powerful logic that admin uses, but filtered to current user
        User user = userService.getUser(userId);

        // We'll create a helper method in UserService for single user
        UserInvestmentDTO portfolio = userService.getUserInvestmentDetails(userId);

        return ResponseEntity.ok(portfolio);
    }

    // Optional: Transaction history
    @GetMapping("/transactions")
    public List<Transaction> getMyTransactions() {
        Long userId = getCurrentUserId();
        return transactionRepo.findByUserId(userId);
    }

}

record OrderRequest(Long investorId, Long propertyId, int tokens, String idempotencyKey) {}
record SellRequest(Long userId, int tokens) {}
record AddressRequest(Long userId, String address) {}
