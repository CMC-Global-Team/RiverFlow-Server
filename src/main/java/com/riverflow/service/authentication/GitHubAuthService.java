package com.riverflow.service.authentication;

import com.riverflow.config.GitHubAuthConfig;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.GitHubSignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import com.riverflow.util.authentication.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GitHubAuthService {

    private final GitHubAuthConfig gitHubAuthConfig;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";
    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";

    @Transactional
    public SignInResponse authenticateWithGitHub(GitHubSignInRequest request) {
        try {
            // Exchange code for access token
            String accessToken = exchangeCodeForToken(request.getCode());
            if (accessToken == null) {
                throw new IllegalArgumentException("Failed to exchange code for GitHub access token");
            }

            // Fetch user info from GitHub
            Map<String, Object> userInfo = fetchGitHubUserInfo(accessToken);
            if (userInfo == null) {
                throw new IllegalArgumentException("Failed to fetch GitHub user info");
            }

            String githubId = String.valueOf(userInfo.get("id"));
            String fullName = (String) userInfo.get("name");
            String login = (String) userInfo.get("login");
            String avatarUrl = (String) userInfo.get("avatar_url");

            // Email might be null if user has private email
            String email = (String) userInfo.get("email");
            if (email == null) {
                email = fetchPrimaryEmail(accessToken);
            }

            if (email == null) {
                throw new IllegalArgumentException("Unable to retrieve email from GitHub account");
            }

            // Use login as name fallback
            if (fullName == null || fullName.isEmpty()) {
                fullName = login;
            }

            // Upsert user
            User user = upsertGitHubUser(email, fullName, avatarUrl, githubId);

            // Load UserDetails for JWT generation
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            String jwtAccessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate avatar URL if avatar data exists in database
            String userAvatarUrl = null;
            if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
                userAvatarUrl = "/user/avatar/" + user.getId();
            } else if (user.getAvatar() != null) {
                userAvatarUrl = user.getAvatar();
            }

            return SignInResponse.builder()
                    .accessToken(jwtAccessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(null)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role("ROLE_USER")
                    .avatar(userAvatarUrl)
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException("GitHub authentication failed: " + ex.getMessage());
        }
    }

    private String exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = Map.of(
                "client_id", gitHubAuthConfig.getClientId(),
                "client_secret", gitHubAuthConfig.getClientSecret(),
                "code", code);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    GITHUB_TOKEN_URL,
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage());
        }

        return null;
    }

    private Map<String, Object> fetchGitHubUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    GITHUB_USER_URL,
                    HttpMethod.GET,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user info: " + e.getMessage());
        }
    }

    private String fetchPrimaryEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<java.util.List<Map<String, Object>>> response = restTemplate.exchange(
                    GITHUB_EMAILS_URL,
                    HttpMethod.GET,
                    entity,
                    (Class<java.util.List<Map<String, Object>>>) (Class<?>) java.util.List.class);

            if (response.getBody() != null) {
                for (Map<String, Object> emailMap : response.getBody()) {
                    Boolean primary = (Boolean) emailMap.get("primary");
                    Boolean verified = (Boolean) emailMap.get("verified");
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailMap.get("email");
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return null
        }

        return null;
    }

    private User upsertGitHubUser(String email, String fullName, String avatarUrl, String githubId) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            // Update minimal fields if changed
            if (fullName != null && !fullName.equals(user.getFullName())) {
                user.setFullName(fullName);
            }
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatar())) {
                user.setAvatar(avatarUrl);
            }
            // Ensure OAuth attributes are set
            user.setOauthProvider(User.OAuthProvider.github);
            user.setOauthId(githubId);
            user.setEmailVerified(Boolean.TRUE);
            if (user.getEmailVerifiedAt() == null) {
                user.setEmailVerifiedAt(LocalDateTime.now());
            }
            return userRepository.save(user);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(null)
                .fullName(fullName != null ? fullName : email)
                .avatar(avatarUrl)
                .status(User.UserStatus.active)
                .role(User.Role.user)
                .oauthProvider(User.OAuthProvider.github)
                .oauthId(githubId)
                .emailVerified(Boolean.TRUE)
                .emailVerifiedAt(LocalDateTime.now())
                .credit(3L)
                .build();
        return userRepository.save(user);
    }
}
