package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.model.User;
import com.product.RealestateTokenization.repository.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService; // <-- Inject JwtService

    public UserService(UserRepo userRepo, PasswordEncoder encoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    // Existing: Email/Password Register
    public User register(String name, String email, String phone, String password) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(encoder.encode(password));
        user.setKycStatus("PENDING");
        user.setCustodyMode("NON_CUSTODIAL");
        user.setRole("INVESTOR");
        return userRepo.save(user);
    }

    // Existing: Login
    public String login(String email, String password) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (encoder.matches(password, user.getPasswordHash())) {
            return jwtService.generateToken(user); // <-- Return real JWT token
        }
        throw new RuntimeException("Invalid login");
    }

    // NEW: Find by email
    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    // NEW: Register via Google
    public User registerViaGoogle(String email, String name, String googleId) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(encoder.encode(googleId)); // dummy hash
        user.setRole("INVESTOR");
        user.setKycStatus("PENDING");
        user.setCustodyMode("NON_CUSTODIAL");
        return userRepo.save(user);
    }

    public User getUser(Long id) {
        return userRepo.findById(id).orElseThrow();
    }
}