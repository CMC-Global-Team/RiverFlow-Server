package com.riverflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI Configuration
 * 
 * Chỉ khởi tạo Spring AI khi có OpenAI API key được cấu hình (không rỗng).
 * Nếu không có API key, server vẫn chạy được nhưng tính năng AI sẽ không hoạt động.
 */
@Configuration
@Slf4j
public class SpringAiConfig {
    
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;
    
    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String model;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;
    
    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;
    
    /**
     * Tạo OpenAiApi bean chỉ khi có API key (không rỗng)
     */
    @Bean
    @ConditionalOnExpression("'${spring.ai.openai.api-key:}' != ''")
    public OpenAiApi openAiApi() {
        log.info("Initializing OpenAI API with model: {}", model);
        return new OpenAiApi(apiKey);
    }
    
    /**
     * Tạo OpenAiChatModel bean chỉ khi có OpenAiApi (tức là có API key)
     */
    @Bean
    @ConditionalOnExpression("'${spring.ai.openai.api-key:}' != ''")
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .withModel(model)
            .withTemperature(temperature.floatValue())
            .withMaxTokens(maxTokens)
            .build();
        log.info("Initializing OpenAiChatModel with model: {}, temperature: {}, maxTokens: {}", 
            model, temperature, maxTokens);
        return new OpenAiChatModel(openAiApi, options);
    }
    
    /**
     * Tạo ChatClient bean chỉ khi có ChatModel (tức là có API key)
     */
    @Bean
    @ConditionalOnExpression("'${spring.ai.openai.api-key:}' != ''")
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("Initializing Spring AI ChatClient with ChatModel: {}", chatModel.getClass().getName());
        return ChatClient.builder(chatModel).build();
    }
}

