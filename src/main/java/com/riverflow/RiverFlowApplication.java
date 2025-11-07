package com.riverflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude = {
        // Exclude Spring AI OpenAI auto-configuration để tránh lỗi khi không có API key
        // Spring AI sẽ được enable lại thông qua SpringAiConfig khi có API key
        org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class
    }
)
public class RiverFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiverFlowApplication.class, args);
    }

}

