package com.riverflow.service.authentication;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.riverflow.config.GoogleAuthConfig;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.GoogleSignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import com.riverflow.util.authentication.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

	private final GoogleAuthConfig googleAuthConfig;
	private final JwtUtil jwtUtil;
	private final CustomUserDetailsService userDetailsService;
	private final UserRepository userRepository;

	@Transactional
	public SignInResponse authenticateWithGoogle(GoogleSignInRequest request) {
		try {
			GoogleIdToken idToken = verifyIdToken(request.getCredential());
			if (idToken == null) {
				throw new IllegalArgumentException("Invalid Google ID token");
			}

			GoogleIdToken.Payload payload = idToken.getPayload();
			String email = payload.getEmail();
			String fullName = (String) payload.get("name");
			String picture = (String) payload.get("picture");
			String sub = payload.getSubject();

			// Upsert user
			User user = upsertGoogleUser(email, fullName, picture, sub);

			// Load UserDetails for JWT generation
			UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

			String accessToken = jwtUtil.generateAccessToken(userDetails);
			String refreshToken = jwtUtil.generateRefreshToken(userDetails);

		user.setLastLoginAt(LocalDateTime.now());
		userRepository.save(user);

		// Generate avatar URL if avatar data exists in database
		// Note: Return /user/avatar/{userId} (without /api prefix) since client baseURL already includes /api
		String avatarUrl = null;
		if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
			avatarUrl = "/user/avatar/" + user.getId();
		} else if (user.getAvatar() != null) {
			// Fallback to legacy URL-based avatar
			avatarUrl = user.getAvatar();
		}

		return SignInResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.expiresIn(null) // Optional: can compute from config if needed
				.userId(user.getId())
				.email(user.getEmail())
				.fullName(user.getFullName())
				.role("ROLE_USER")
				.avatar(avatarUrl)
				.build();
		} catch (Exception ex) {
			log.error("Google authentication failed: {}", ex.getMessage());
			throw new RuntimeException("Google authentication failed");
		}
	}	private GoogleIdToken verifyIdToken(String idTokenString) throws GeneralSecurityException {
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),
					GsonFactory.getDefaultInstance()
			)
					.setAudience(Collections.singletonList(googleAuthConfig.getClientId()))
					.build();
			return verifier.verify(idTokenString);
		} catch (GeneralSecurityException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize Google verifier", e);
		}
	}

	private User upsertGoogleUser(String email, String fullName, String picture, String sub) {
		Optional<User> existing = userRepository.findByEmail(email);
		if (existing.isPresent()) {
			User user = existing.get();
			// Update minimal fields if changed
			if (fullName != null && !fullName.equals(user.getFullName())) {
				user.setFullName(fullName);
			}
			if (picture != null && !picture.equals(user.getAvatar())) {
				user.setAvatar(picture);
			}
			// Ensure OAuth attributes are set
			user.setOauthProvider(User.OAuthProvider.google);
			user.setOauthId(sub);
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
				.avatar(picture)
				.status(User.UserStatus.active)
				.oauthProvider(User.OAuthProvider.google)
				.oauthId(sub)
				.emailVerified(Boolean.TRUE)
				.emailVerifiedAt(LocalDateTime.now())
				.build();
		return userRepository.save(user);
	}
}


