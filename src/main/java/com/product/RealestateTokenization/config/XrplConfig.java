package com.product.RealestateTokenization.config;

import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xrpl.xrpl4j.client.XrplClient;

@Configuration
public class XrplConfig {

    @Value("${xrpl.network:testnet}")
    private String network;

    @Bean
    public XrplClient xrplClient() {
        String url = "testnet".equalsIgnoreCase(network)
                ? "https://s.altnet.rippletest.net:51234"
                : "https://s1.ripple.com:51234";

        return new XrplClient(HttpUrl.parse(url));
    }
}