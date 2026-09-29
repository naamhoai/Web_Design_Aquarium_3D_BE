package com.aquarium.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token dạng chuỗi ngẫu nhiên 256-bit. DB chỉ lưu giá trị băm SHA-256
 * (cột {@code token}) nên dù DB bị lộ kẻ tấn công cũng không dùng được token.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked")
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Token mới thay thế token này khi xoay vòng (null nếu bị thu hồi vì lý do khác). */
    @Column(name = "replaced_by")
    private UUID replacedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
