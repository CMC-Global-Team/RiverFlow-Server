package com.riverflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    @Value("${openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Bean
    public WebClient openAiWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        return builder
                .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
                    // Do not expose stacktrace; propagate status for handler
                    return reactor.core.publisher.Mono.just(clientResponse);
                }))
                .build();
    }
}



