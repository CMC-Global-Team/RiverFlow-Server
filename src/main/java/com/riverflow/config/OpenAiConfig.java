package com.riverflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    // Switch to Gemini service configuration (keep bean name for compatibility)
    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Bean
    public WebClient openAiWebClient() {
        // Gemini uses API key via query parameter (?key=...), not Authorization header
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse ->
                        reactor.core.publisher.Mono.just(clientResponse)))
                .build();
    }
}

