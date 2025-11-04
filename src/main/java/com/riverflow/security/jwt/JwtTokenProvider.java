package com.riverflow.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    // Đọc các giá trị từ application.properties
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    /**
     * Tạo một Access Token mới cho người dùng.
     */
    public String generateAccessToken(String username) {
        return buildToken(username, accessTokenExpirationMs);
    }

    /**
     * Tạo một Refresh Token mới cho người dùng.
     */
    public String generateRefreshToken(String username) {
        return buildToken(username, refreshTokenExpirationMs);
    }

    /**
     * Phương thức chung để xây dựng token
     */
    private String buildToken(String username, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username) // Đặt username (email) làm subject
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey()) // Ký tên bằng chìa khóa bí mật
                .compact();
    }

    /**
     * Giải mã token để lấy Tên đăng nhập (email).
     */
    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    /**
     * Xác thực token (kiểm tra chữ ký, thời gian hết hạn).
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            logger.error("JWT đã hết hạn: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            logger.error("JWT không hợp lệ: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Lấy một "claim" (thông tin) cụ thể từ token.
     */
    private <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    /**
     * Chuyển đổi chuỗi secret (Base64) thành một đối tượng SecretKey an toàn
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}