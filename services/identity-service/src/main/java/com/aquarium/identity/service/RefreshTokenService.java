package com.aquarium.identity.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.JwtProperties;
import com.aquarium.identity.entity.RefreshToken;
import com.aquarium.identity.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Quản lý refresh token theo mô hình "rotation + reuse detection":
 * mỗi lần làm mới sẽ thu hồi token cũ và cấp token mới. Nếu một token đã bị thu hồi
 * lại được dùng (dấu hiệu token bị đánh cắp) thì thu hồi TOÀN BỘ phiên của người dùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    /** Cho phép 2 tab cùng làm mới gần như đồng thời mà không bị coi là tấn công. */
    private static final Duration ROTATION_GRACE = Duration.ofSeconds(20);
    private static final Pattern TOKEN_FORMAT = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;

    public record Issued(String rawToken, RefreshToken entity) {
    }

    @Transactional
    public Issued issue(UUID userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256(raw))
                .expiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl()))
                .revoked(false)
                .build();
        return new Issued(raw, repository.save(token));
    }

    /**
     * Xoay vòng token. Trả về userId + token mới.
     * noRollbackFor: khi phát hiện dùng lại token, lệnh thu hồi toàn bộ phiên phải được commit
     * dù sau đó vẫn ném lỗi 401.
     */
    @Transactional(noRollbackFor = AppException.class)
    public Rotation rotate(String rawToken) {
        if (rawToken == null || !TOKEN_FORMAT.matcher(rawToken).matches()) {
            throw invalid();
        }
        RefreshToken current = repository.findByTokenHashForUpdate(sha256(rawToken)).orElseThrow(RefreshTokenService::invalid);
        Instant now = Instant.now();

        if (Boolean.TRUE.equals(current.getRevoked())) {
            boolean justRotated = current.getReplacedBy() != null
                    && current.getRevokedAt() != null
                    && current.getRevokedAt().isAfter(now.minus(ROTATION_GRACE));
            if (!justRotated) {
                int revoked = repository.revokeAllForUser(current.getUserId(), now);
                log.warn("Phát hiện refresh token bị dùng lại cho user {} — đã thu hồi {} phiên", current.getUserId(), revoked);
            }
            throw invalid();
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw invalid();
        }

        Issued next = issue(current.getUserId());
        current.setRevoked(true);
        current.setRevokedAt(now);
        current.setReplacedBy(next.entity().getId());
        return new Rotation(current.getUserId(), next.rawToken());
    }

    public record Rotation(UUID userId, String newRawToken) {
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || !TOKEN_FORMAT.matcher(rawToken).matches()) {
            return;
        }
        repository.findByTokenHash(sha256(rawToken)).ifPresent(token -> {
            if (!Boolean.TRUE.equals(token.getRevoked())) {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
            }
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    /** Dọn token hết hạn hằng ngày lúc 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        int deleted = repository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
        if (deleted > 0) {
            log.info("Đã xóa {} refresh token hết hạn", deleted);
        }
    }

    private static AppException invalid() {
        return new AppException(ErrorCode.INVALID_TOKEN);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
