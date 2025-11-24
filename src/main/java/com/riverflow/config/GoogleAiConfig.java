package com.riverflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GoogleAiConfig {

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Bean
    public WebClient geminiWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("x-goog-api-key", apiKey);
        }
        return builder
                .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse ->
                        reactor.core.publisher.Mono.just(clientResponse)))
                .build();
    }
}

