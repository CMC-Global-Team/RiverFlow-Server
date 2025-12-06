package com.riverflow.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class GoogleAuthConfig {

	@Value("${app.google.client-id:}")
	private String clientId;

	@Value("${app.google.client-secret:}")
	private String clientSecret;
}
