package com.product.RealestateTokenization.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.product.RealestateTokenization.model.User;
import com.product.RealestateTokenization.service.JwtService;
import com.product.RealestateTokenization.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flutter/auth")
public class GoogleAuthController {

    private final JwtService jwtService;
    private final UserService userService;

    public GoogleAuthController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body)
            throws Exception {

        String idTokenString = body.get("idToken");

        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        new JacksonFactory()
                )
                        .setAudience(List.of("520499947967-tvkj6oi6j4aas3k2s2ep79kmi5g4p4q2.apps.googleusercontent.com"))
                        .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            return ResponseEntity.status(401).body("Invalid Google token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String googleId = payload.getSubject();

        User user = userService.findByEmail(email)
                .orElseGet(() ->
                        userService.registerViaGoogle(email, name, googleId)
                );

        String jwt = jwtService.generateToken(user);

        System.out.print(jwt);
        return ResponseEntity.ok(Map.of(
                "token", jwt
        ));
    }
}

