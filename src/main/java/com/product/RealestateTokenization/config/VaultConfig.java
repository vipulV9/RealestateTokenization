package com.product.RealestateTokenization.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

@Configuration
public class VaultConfig {

    @Bean
    public VaultTemplate vaultTemplate() {
        // Load .env from project root
        Dotenv dotenv = Dotenv.configure()
                .directory("C:\\Users\\vipul\\Downloads\\RealestateTokenization\\RealestateTokenization")
                .filename(".env")
                .ignoreIfMissing()
                .load();

        String token = dotenv.get("VAULT_TOKEN");
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "VAULT_TOKEN is missing!\n" +
                            "1. Run: cd C:\\vault && vault.exe server -dev\n" +
                            "2. Copy the 'Root Token'\n" +
                            "3. Paste into .env file at:\n" +
                            "   C:\\Users\\vipul\\Downloads\\RealestateTokenization\\RealestateTokenization\\.env"
            );
        }

        // Use 127.0.0.1, not "vault"
        VaultEndpoint endpoint = VaultEndpoint.create("127.0.0.1", 8200);
        endpoint.setScheme("http");

        return new VaultTemplate(endpoint, new TokenAuthentication(token));
    }
}