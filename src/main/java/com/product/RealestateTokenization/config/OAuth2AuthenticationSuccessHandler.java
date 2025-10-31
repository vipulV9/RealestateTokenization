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

        // STOP FURTHER PROCESSING
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String googleId = oidcUser.getSubject();

        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            response.getWriter().write("{\"error\": \"Email not verified\"}");
            return;
        }

        User user = userService.findByEmail(email)
                .orElseGet(() -> userService.registerViaGoogle(email, name, googleId));

        String jwt = jwtService.generateToken(user);

        response.getWriter().write("{\"token\": \"" + jwt + "\"}");

        // CRITICAL: PREVENT DEFAULT REDIRECT
        response.flushBuffer();
    }
}
