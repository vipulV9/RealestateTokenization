package com.product.RealestateTokenization.controller;

import com.product.RealestateTokenization.model.User;
import com.product.RealestateTokenization.repository.UserRepo;
import com.product.RealestateTokenization.service.JwtService;
import com.product.RealestateTokenization.service.KycSessionService;
import com.product.RealestateTokenization.service.TwilioService;
import com.product.RealestateTokenization.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    @Autowired
    private TwilioService twilioService;

    private final UserRepo userRepo;

    private final KycSessionService kycSessionService;
    public AuthController(UserService userService, UserRepo userRepo, KycSessionService kycSessionService) {
        this.userService = userService;
        this.userRepo = userRepo;
        this.kycSessionService = kycSessionService;
    }



    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest req) {
        User user = userService.register(req.name(), req.email(), req.phone(), req.password());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/add-phone")
    public ResponseEntity<?> updatePhone(
            @RequestBody Map<String, String> payload
    ) {

        Long userId = getCurrentUserId();  // extracted from JWT securely
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String phone = payload.get("phone");

        if (phone == null || phone.length() != 13 || !phone.startsWith("+91")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid phone format"));
        }

        if (user.isPhoneVerified()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Phone already verified"));
        }

        user.setPhone(phone);
        user.setPhoneVerified(true);

        userRepo.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Phone updated successfully",
                "phone", user.getPhone()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest req) {
        String token = userService.login(req.email(), req.password());
        return ResponseEntity.ok(token);          // <-- plain string, not JSON
    }


    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> oauth2Error(@RequestParam String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> sendOtp(@RequestBody SendOtpRequest req) {
        String phone = req.phone().replaceAll("[^0-9]", "").trim();

        if (phone.length() != 10) {
            return ResponseEntity.badRequest()
                    .body(new OtpResponse("Invalid Indian phone number", false));
        }

        boolean sent = twilioService.sendOtp("+91" + phone);
        if (sent) {
            return ResponseEntity.ok(new OtpResponse("OTP sent successfully", true));
        } else {
            return ResponseEntity.status(500)
                    .body(new OtpResponse("Failed to send OTP", false));
        }
    }

    // Verify OTP (during registration or login)
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest req) {
        String phone = req.phone().replaceAll("[^0-9]", "").trim();

        if (phone.length() != 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid phone"));
        }

        boolean valid = twilioService.verifyOtp("+91" + phone, req.code());

        if (valid) {
            // Optional: Auto-register or mark phone as verified
            userService.markPhoneAsVerified("+91" + phone); // implement below
            return ResponseEntity.ok(Map.of("message", "Phone verified", "verified", true));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));
        }
    }


    @PostMapping("/kyc/start")
    public ResponseEntity<?> startKyc(@RequestBody StartKycRequest req) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Please login first"));
        }

        User currentUser = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String phone = req.phone().replaceAll("[^0-9]", "").trim();
        String pan = req.pan().trim().toUpperCase();

        if (phone.length() != 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid phone number"));
        }
        if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]{1}")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid PAN format"));
        }

        if (currentUser.isPhoneVerified()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone already verified"));
        }

        boolean sent = twilioService.sendOtp("+91" + phone);
        if (!sent) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to send OTP"));
        }

        String sessionId = kycSessionService.createSession(pan, "+91" + phone);

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully",
                "sessionId", sessionId,
                "expiresInMinutes", 10
        ));
    }

    @PostMapping("/kyc/complete")
    public ResponseEntity<?> completeKyc(@RequestBody CompleteKycRequest req) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Please login first"));
        }

        User currentUser = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        KycSessionService.KycSessionData data = kycSessionService.getAndDelete(req.sessionId());
        if (data == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired session"));
        }

        boolean valid = twilioService.verifyOtp(data.phone(), req.code());
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));
        }

        if (currentUser.isPhoneVerified()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone already verified"));
        }

        // UPDATE USER
        currentUser.setPhone(data.phone());
        currentUser.setPhoneVerified(true);
        currentUser.setPanNumber(data.pan());
        currentUser.setKycStatus("COMPLETED");
        userRepo.save(currentUser);

        // Fresh token with updated claims
//        String newToken = jwtService.generateToken(currentUser);

        return ResponseEntity.ok(Map.of(
                "message", "KYC completed successfully!",
                "userId", currentUser.getId(),
                "phone", currentUser.getPhone(),
                "panNumber", currentUser.getPanNumber(),
                "kycStatus", "COMPLETED"
        ));
    }
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

record RegisterRequest(String name, String email, String phone, String password) {}
record LoginRequest(String email, String password) {}

// Inside AuthController.java or new file
record SendOtpRequest(String phone) {}
record VerifyOtpRequest(String phone, String code) {}
record OtpResponse(String message, boolean success) {}
record StartKycRequest(String phone, String pan) {}
record CompleteKycRequest(String sessionId, String code) {}
