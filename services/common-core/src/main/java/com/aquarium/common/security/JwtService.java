package com.aquarium.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Phát hành và xác minh JWT access token.
 * <ul>
 *   <li>Chỉ chấp nhận HS256 ký bằng khóa bí mật cấu hình qua môi trường.</li>
 *   <li>Bắt buộc đúng issuer, audience và claim {@code typ=access} — refresh token
 *       (dạng chuỗi ngẫu nhiên, không phải JWT) không thể dùng thay access token.</li>
 *   <li>Access token sống ngắn (mặc định 15 phút).</li>
 * </ul>
 */
public class JwtService {

    static final String CLAIM_TYPE = "typ";
    static final String TYPE_ACCESS = "access";
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final int MIN_SECRET_LENGTH = 32;
    private static final Set<String> ALLOWED_ROLES = Set.of(
            AuthenticatedUser.ROLE_CUSTOMER, AuthenticatedUser.ROLE_SUPPLIER,
            AuthenticatedUser.ROLE_ADMIN, AuthenticatedUser.ROLE_TECHNICIAN);
    /** Khóa mặc định cũ đã bị commit công khai trong lịch sử git — tuyệt đối không dùng lại. */
    private static final Set<String> KNOWN_LEAKED_SECRETS = Set.of(
            "super-secret-aquarium-jwt-token-key-2026-min-32-chars-length");

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình jwt.secret. Hãy đặt biến môi trường JWT_SECRET (>= 32 ký tự ngẫu nhiên).");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET quá ngắn: cần tối thiểu " + MIN_SECRET_LENGTH + " ký tự.");
        }
        if (KNOWN_LEAKED_SECRETS.contains(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET đang dùng khóa mặc định đã bị lộ trong mã nguồn. Hãy tạo khóa ngẫu nhiên mới.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTokenTtl())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new InvalidTokenException("Token rỗng hoặc quá dài");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getIssuer())
                    .requireAudience(properties.getAudience())
                    .clockSkewSeconds(properties.getClockSkew().toSeconds())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new InvalidTokenException("Sai loại token");
            }
            String role = claims.get("role", String.class);
            if (role == null || !ALLOWED_ROLES.contains(role)) {
                throw new InvalidTokenException("Vai trò không hợp lệ");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            return new AuthenticatedUser(userId, claims.get("email", String.class), role);
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new InvalidTokenException("Token không hợp lệ");
        }
    }
}
