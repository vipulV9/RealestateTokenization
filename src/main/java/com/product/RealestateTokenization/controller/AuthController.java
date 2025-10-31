package com.product.RealestateTokenization.controller;

import com.product.RealestateTokenization.model.User;
import com.product.RealestateTokenization.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest req) {
        User user = userService.register(req.name(), req.email(), req.phone(), req.password());
        return ResponseEntity.ok(user);
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
}

record RegisterRequest(String name, String email, String phone, String password) {}
record LoginRequest(String email, String password) {}