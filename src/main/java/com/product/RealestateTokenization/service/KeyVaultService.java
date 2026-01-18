package com.product.RealestateTokenization.service;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KeyVaultService {

    private final SecretClient secretClient;

    public KeyVaultService(@Value("${spring.cloud.azure.keyvault.secret.endpoint}") String vaultUri) {
        this.secretClient = new SecretClientBuilder()
                .vaultUrl(vaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    public void storeSeed(Long userId, String seed) {
        String secretName = "investor-xrpl-seed-" + userId;
        secretClient.setSecret(secretName, seed);
    }

    public String getSeed(String secretName) {
        return secretClient
                .getSecret(secretName)
                .getValue();
    }


    // Optional: delete when user account is removed
    public void deleteSeed(Long userId) {
        String secretName = "investor-xrpl-seed-" + userId;
        secretClient.beginDeleteSecret(secretName).waitForCompletion();
    }
}