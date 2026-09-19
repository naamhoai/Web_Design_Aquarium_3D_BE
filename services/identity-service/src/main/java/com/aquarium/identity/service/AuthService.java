package com.aquarium.identity.service;

import com.aquarium.identity.dto.AuthResponse;
import com.aquarium.identity.dto.LoginRequest;
import com.aquarium.identity.dto.RegisterRequest;
import com.aquarium.identity.dto.UserProfileResponse;
import com.aquarium.identity.entity.User;

import java.util.UUID;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse getProfile(User currentUser);
}
