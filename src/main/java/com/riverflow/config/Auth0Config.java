package com.riverflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Auth0 OIDC properties.
 * Maps properties from application.properties with prefix "app.auth0"
 */
@Configuration
@ConfigurationProperties(prefix = "app.auth0")
@Getter
@Setter
public class Auth0Config {

    /**
     * Auth0 domain (e.g., your-tenant.auth0.com)
     */
    private String domain;

    /**
     * Auth0 application client ID
     */
    private String clientId;

    /**
     * Auth0 application client secret
     */
    private String clientSecret;

    /**
     * Auth0 API audience (API identifier)
     */
    private String audience;

    /**
     * Get the issuer URI for Auth0
     * 
     * @return The full issuer URI (https://{domain}/)
     */
    public String getIssuerUri() {
        if (domain == null || domain.isEmpty()) {
            return null;
        }
        return "https://" + domain + "/";
    }

    /**
     * Get the JWKS URI for Auth0
     * 
     * @return The full JWKS URI (https://{domain}/.well-known/jwks.json)
     */
    public String getJwksUri() {
        if (domain == null || domain.isEmpty()) {
            return null;
        }
        return "https://" + domain + "/.well-known/jwks.json";
    }
}
