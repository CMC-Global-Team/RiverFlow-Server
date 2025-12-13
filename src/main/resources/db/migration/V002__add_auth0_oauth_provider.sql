-- Add 'auth0' to oauth_provider ENUM for Auth0 OIDC SSO support
ALTER TABLE users MODIFY COLUMN oauth_provider ENUM('email', 'google', 'github', 'facebook', 'auth0') NOT NULL DEFAULT 'email';
