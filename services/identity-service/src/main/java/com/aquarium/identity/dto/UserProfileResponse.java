package com.aquarium.identity.dto;

import com.aquarium.identity.entity.UserRole;
import com.aquarium.identity.entity.UserStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private Boolean emailVerified;
    private Instant createdAt;
}
