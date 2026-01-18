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

        boolean isAdmin = user.getRole().equalsIgnoreCase("ADMIN");

        String redirectUrl;

        if (isAdmin) {
            // ADMIN → 5173
            redirectUrl = "http://localhost:5174/admin?token=" + jwt;
        } else {
            // INVESTOR → 5174
            redirectUrl = "http://localhost:5173/oauth2-redirect?token=" + jwt;
        }

        response.sendRedirect(redirectUrl);
    }
}
