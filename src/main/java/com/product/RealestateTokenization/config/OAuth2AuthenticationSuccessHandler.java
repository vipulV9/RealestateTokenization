package com.product.RealestateTokenization.config;

import com.product.RealestateTokenization.model.User;
import com.product.RealestateTokenization.service.JwtService;
import com.product.RealestateTokenization.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Autowired
    private UserService userService;


    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String googleId = oidcUser.getSubject();

        User user = userService.findByEmail(email)
                .orElseGet(() -> userService.registerViaGoogle(email, name, googleId));

        String jwt = jwtService.generateToken(user);

        // Detect if this user is admin
        boolean isAdmin = user.getRole().equals("ADMIN");

        String redirectUrl = isAdmin
                ? "http://127.0.0.1:5500/adminsite.html?token=" + jwt
                : "http://127.0.0.1:5500/payment.html?token=" + jwt;

        response.sendRedirect(redirectUrl);
    }


}
