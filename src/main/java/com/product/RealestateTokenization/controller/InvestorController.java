package com.product.RealestateTokenization.controller;


import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.TransactionRepo;
import com.product.RealestateTokenization.service.*;
import com.razorpay.Order;
import jakarta.mail.MessagingException;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.json.JSONObject;
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

    @Autowired
    EmailService emailService;

    public InvestorController(PropertyService propertyService, WalletService walletService, PaymentService paymentService) {
        this.propertyService = propertyService;
        this.walletService = walletService;
        this.paymentService = paymentService;
    }

    @GetMapping("/properties")
    public List<Property> listProperties() {
        return propertyService.getOnChainProperties();
    }

    @GetMapping("/properties/rent")
    public List<Property> getallrentedprop()
    {
        return propertyService.getallRentedProperty();
    }

    @GetMapping("/nearby/rent")
    public List<Property> getnearby(
            @RequestParam double lat,
            @RequestParam double lng
    )
    {
        return propertyService.getNormalPropertiesNearby(lat,lng);
    }


    @GetMapping("/properties/{id}")
    public Property getProperty(@PathVariable Long id) {
        return propertyService.getProperty(id);
    }

    @GetMapping("/properties/by-type")
    public ResponseEntity<List<Property>> getPropertiesByType(
            @RequestParam String type
    )
    {
        return ResponseEntity.ok(propertyService.getPropByType(type));
    }

    @GetMapping("/properties/by-types")
    public List<Property> getByTypes(@RequestParam String types) {
        return propertyService.getPropsByTypes(types);
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

    @GetMapping("/rent/history")
    public List<Transaction> getMyRentHistory() {
        Long userId = getCurrentUserId();
        return transactionRepo.findByUserIdAndStatus(userId, TransactionStatus.RENT_DISTRIBUTION);
    }
//    @PostMapping("/properties/{id}/express-interest")
//    public ResponseEntity<?> expressInterest(
//            @PathVariable Long id,
//            @RequestBody Map<String, Object> payload) throws Exception {
//
//        Long userId = getCurrentUserId();
//        int tokens = (Integer) payload.get("tokens");
//
//        User user = userService.getUser(userId);
//        Property property = propertyService.getProperty(id);
//
//        if (tokens <= 0 || tokens > property.getAvailableTokens()) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid number of tokens"));
//        }
//
//        // Prevent duplicate interest
//        if (transactionRepo.existsByUserIdAndPropertyIdAndStatusIn(userId, id,
//                List.of(TransactionStatus.PENDING_INTEREST))) {
//            return ResponseEntity.badRequest().body(Map.of("error", "You already expressed interest"));
//        }
//        double baseAmount = tokens * property.getTokenPrice();
//        double totalAmount = baseAmount;
//        boolean needsFee = "CUSTODIAL".equals(user.getCustodyMode()) && !walletService.existsByUserId(userId);
//        if (needsFee) totalAmount += 800;
//
//        String referenceId = "INV_" + userId + "_" + id + "_" + System.currentTimeMillis();
//
//        // Create Payment Link via your method
//        JSONObject linkResponse = paymentService.createPaymentLink(
//                user.getName(),
//                user.getEmail(),
//                user.getPhone(),
//                totalAmount,
//                referenceId,
//                property.getName());
//
//        String paymentLinkUrl = linkResponse.getString("short_url");
//        String paymentLinkId = linkResponse.getString("id");
//
//        // Save Transaction
//        Transaction tx = new Transaction();
//        tx.setUserId(userId);
//        tx.setPropertyId(id);
//        tx.setTokens(tokens);
//        tx.setAmount(totalAmount);
//        tx.setRazorpayOrderId(paymentLinkId);           // This is Payment Link ID
//        tx.setIdempotencyKey(referenceId);
//        tx.setStatus(TransactionStatus.PENDING_INTEREST);
//        tx.setWalletCreationFee(needsFee);
//        tx.setWalletFeeAmount(needsFee ? 800.0 : 0.0);
//        transactionRepo.save(tx);
//
//        // Send Email
//        emailService.sendInterestEmail(
//                user.getEmail(),
//                user.getName(),
//                property.getName(),
//                property.getTokenSymbol(),
//                tokens,
//                property.getTokenPrice(),
//                totalAmount,
//                paymentLinkUrl,
//                needsFee
//        );
//
//        return ResponseEntity.ok(Map.of(
//                "message", "Thank you! Please check your email for payment instructions.",
//                "payment_link", paymentLinkUrl
//        ));
//    }
@PostMapping("/properties/{id}/buy")  // CHANGED: Renamed from express-interest for clarity
public ResponseEntity<?> buyTokens(
        @PathVariable Long id,
        @RequestBody Map<String, Object> payload) throws Exception {

    Long userId = getCurrentUserId();
    int tokens = (Integer) payload.get("tokens");

    User user = userService.getUser(userId);
    Property property = propertyService.getProperty(id);

    if (tokens <= 0 || tokens > property.getAvailableTokens()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid number of tokens"));
    }

//    // Prevent duplicate pending buys
//    if (transactionRepo.existsByUserIdAndPropertyIdAndStatusIn(userId, id,
//            List.of(TransactionStatus.PAYMENT_RECEIVED_PENDING_ADMIN))) {
//        return ResponseEntity.badRequest().body(Map.of("error", "You already have a pending purchase"));
//    }

    // Calculate amounts (include wallet fee if needed)
    double baseAmount = tokens * property.getTokenPrice();
    double totalAmount = baseAmount;
    boolean needsFee = "CUSTODIAL".equals(user.getCustodyMode()) && !walletService.existsByUserId(userId);
    if (needsFee) totalAmount += 800;

    String referenceId = "INV_" + userId + "_" + id + "_" + System.currentTimeMillis();

    // Save Transaction (pending admin confirm after offline payment)
    Transaction tx = new Transaction();
    tx.setUserId(userId);
    tx.setPropertyId(id);
    tx.setTokens(tokens);
    tx.setAmount(totalAmount);
    tx.setIdempotencyKey(referenceId);
    tx.setStatus(TransactionStatus.PAYMENT_RECEIVED_PENDING_ADMIN);
    tx.setWalletCreationFee(needsFee);
    tx.setWalletFeeAmount(needsFee ? 800.0 : 0.0);
    transactionRepo.save(tx);

    // Send Email with SPV bank details and PAS-4
    emailService.sendInvestmentEmail(
            user.getEmail(),
            user.getName(),
            property.getName(),
            property.getTokenSymbol(),           // SPV Name (token symbol)
            tokens,                              // Shares applied
            property.getTokenPrice(),            // Price per share
            totalAmount,                         // Total investment amount (tokens * price + fee if any)
            java.time.LocalDate.now(),           // Today's date as investmentDate
            "U63120HR2025PTC139086"              // Replace with your actual CIN, or inject via @Value
    );

    return ResponseEntity.ok(Map.of(
            "message", "Thank you! our team will contact you shortly"
    ));
}

    @PostMapping("/listpropertyreq")
    public ResponseEntity<?> submitPropertyListingInterest(
            @RequestBody PropertyListingInterestReq req) throws MessagingException {

        // Simple validation
        if (req.name() == null || req.email() == null || req.phone() == null || req.city() == null) {
            return ResponseEntity.badRequest()
                    .body("Name, email, phone and city are required");
        }

        // Send email to YOU (admin)
        emailService.sendPropertyListingInterestEmail(
                "vipulyadav3k@gmail.com",  // Change to your email
                req.name(),
                req.email(),
                req.phone(),
                req.city(),
                req.propertyType(),
                req.approxValue(),
                req.message()
        );

        return ResponseEntity.ok("""
            {
                "message": "Thank you! We have received your interest. Our team will contact you within 24-48 hours."
            }
            """);
    }
}

record OrderRequest(Long investorId, Long propertyId, int tokens, String idempotencyKey) {}
record SellRequest(Long userId, int tokens) {}
record AddressRequest(Long userId, String address) {}
record PropertyListingInterestReq(
        String name,
        String email,
        String phone,
        String city,
        String propertyType,        // e.g., Residential, Commercial, Plot, etc.
        Double approxValue,         // in lakhs or crores
        String message             // optional extra notes
) {}
