package com.product.RealestateTokenization.util;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticConfig {
    @Value("${spring.elasticsearch.uris}")
    private String elasticUrl;

    @Value("${spring.elasticsearch.api-key}")
    private String apiKey;
    @Bean
    public RestClient restClient() {
        return RestClient.builder(
                HttpHost.create(elasticUrl)
        ).setDefaultHeaders(new Header[]{
                new BasicHeader("Authorization", apiKey)
        }).build();
    }
}