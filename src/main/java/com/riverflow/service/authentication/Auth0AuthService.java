package com.riverflow.service.authentication;

import com.riverflow.config.Auth0Config;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.Auth0SignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import com.riverflow.util.authentication.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling Auth0 OIDC authentication.
 * Validates ID tokens from Auth0 and manages user creation/update.
 */
@Service
@RequiredArgsConstructor
public class Auth0AuthService {

    private final Auth0Config auth0Config;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    private JwtDecoder jwtDecoder;

    /**
     * Authenticate a user with an Auth0 ID token.
     * Validates the token, extracts user information, and creates/updates the user
     * in the database.
     * 
     * @param request The Auth0 sign-in request containing the ID token
     * @return SignInResponse with access and refresh tokens
     */
    @Transactional
    public SignInResponse authenticateWithAuth0(Auth0SignInRequest request) {
        try {
            // Validate and decode the Auth0 ID token
            Jwt jwt = getJwtDecoder().decode(request.getIdToken());

            // Extract user claims from the ID token
            String email = jwt.getClaimAsString("email");
            String sub = jwt.getSubject();
            String name = jwt.getClaimAsString("name");
            String picture = jwt.getClaimAsString("picture");
            Boolean emailVerified = jwt.getClaim("email_verified");

            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Email claim is missing from Auth0 token");
            }

            // Upsert user in database
            User user = upsertAuth0User(email, name, picture, sub, emailVerified);

            // Load UserDetails for internal JWT generation
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            // Generate internal JWT tokens
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Update last login timestamp
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate avatar URL if avatar data exists in database
            String avatarUrl = null;
            if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
                avatarUrl = "/user/avatar/" + user.getId();
            } else if (user.getAvatar() != null) {
                avatarUrl = user.getAvatar();
            }

            return SignInResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(null)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole() != null ? user.getRole().name().toUpperCase() : "USER")
                    .credit(user.getCredit() != null ? user.getCredit() : 0L)
                    .avatar(avatarUrl)
                    .build();

        } catch (JwtException ex) {
            throw new RuntimeException("Invalid Auth0 ID token: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Auth0 authentication failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get or create the JWT decoder for Auth0 token validation.
     * Uses Auth0's JWKS endpoint to validate token signatures.
     */
    private JwtDecoder getJwtDecoder() {
        if (jwtDecoder == null) {
            String issuerUri = auth0Config.getIssuerUri();
            if (issuerUri == null || issuerUri.isEmpty()) {
                throw new IllegalStateException("Auth0 domain is not configured");
            }
            jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);
        }
        return jwtDecoder;
    }

    /**
     * Create or update a user from Auth0 authentication.
     */
    private User upsertAuth0User(String email, String fullName, String picture, String sub, Boolean emailVerified) {
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            User user = existing.get();

            // Update user info if changed
            if (fullName != null && !fullName.equals(user.getFullName())) {
                user.setFullName(fullName);
            }
            if (picture != null && !picture.equals(user.getAvatar())) {
                user.setAvatar(picture);
            }

            // Ensure OAuth attributes are set for Auth0
            user.setOauthProvider(User.OAuthProvider.auth0);
            user.setOauthId(sub);

            // Update email verification status
            if (Boolean.TRUE.equals(emailVerified)) {
                user.setEmailVerified(Boolean.TRUE);
                if (user.getEmailVerifiedAt() == null) {
                    user.setEmailVerifiedAt(LocalDateTime.now());
                }
            }

            return userRepository.save(user);
        }

        // Create new user
        User user = User.builder()
                .email(email)
                .passwordHash(null) // OAuth users don't have passwords
                .fullName(fullName != null ? fullName : email)
                .avatar(picture)
                .status(User.UserStatus.active)
                .role(User.Role.user)
                .oauthProvider(User.OAuthProvider.auth0)
                .oauthId(sub)
                .emailVerified(Boolean.TRUE.equals(emailVerified))
                .emailVerifiedAt(Boolean.TRUE.equals(emailVerified) ? LocalDateTime.now() : null)
                .credit(3L) // Default credits for new users
                .build();

        return userRepository.save(user);
    }
}
