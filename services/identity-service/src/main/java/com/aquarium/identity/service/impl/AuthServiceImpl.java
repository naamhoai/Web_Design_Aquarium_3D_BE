package com.aquarium.identity.service.impl;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.JwtService;
import com.aquarium.identity.dto.AuthResponse;
import com.aquarium.identity.dto.LoginRequest;
import com.aquarium.identity.dto.RegisterRequest;
import com.aquarium.identity.dto.ChangePasswordRequest;
import com.aquarium.identity.dto.UpdateProfileRequest;
import com.aquarium.identity.dto.UserProfileResponse;
import com.aquarium.identity.entity.User;
import com.aquarium.identity.entity.UserRole;
import com.aquarium.identity.entity.UserStatus;
import com.aquarium.identity.repository.UserRepository;
import com.aquarium.identity.service.AuthService;
import com.aquarium.identity.service.LoginAttemptService;
import com.aquarium.identity.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    /** Hash giả để so khớp khi email không tồn tại — thời gian phản hồi như nhau, tránh dò email. */
    private final String dummyPasswordHash;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request, String clientIp) {
        loginAttemptService.checkRegistrationAllowed(clientIp);

        String email = normalizeEmail(request.getEmail());
        ensurePasswordFitsBcrypt(request.getPassword());
        String phone = request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.USER_EXISTED, "Email này đã được đăng ký tài khoản");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new AppException(ErrorCode.USER_EXISTED, "Số điện thoại này đã được đăng ký tài khoản");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(phone)
                .role(UserRole.CUSTOMER)      // luôn là CUSTOMER — không cho client tự chọn vai trò
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        loginAttemptService.recordRegistration(clientIp);
        log.info("Đăng ký tài khoản mới: {}", savedUser.getId());
        return issueTokens(savedUser);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request.getEmail());
        loginAttemptService.checkLoginAllowed(email, clientIp);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        String candidateHash = user != null ? user.getPasswordHash() : dummyPasswordHash;
        boolean passwordOk = passwordEncoder.matches(request.getPassword(), candidateHash);

        if (user == null || !passwordOk) {
            loginAttemptService.recordLoginFailure(email, clientIp);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác");
        }
        if (user.getDeletedAt() != null || user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt");
        }

        loginAttemptService.recordLoginSuccess(email);
        return issueTokens(user);
    }

    @Override
    @Transactional(noRollbackFor = AppException.class)
    public AuthResult refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
        if (user.getDeletedAt() != null || user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenService.revokeAllForUser(user.getId());
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        // Vai trò/trạng thái được đọc lại từ DB mỗi lần làm mới → thay đổi quyền có hiệu lực nhanh
        return new AuthResult(buildResponse(user), rotation.newRawToken());
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return mapToProfile(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isEmpty()) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Họ tên không được để trống");
            }
            user.setFullName(fullName);
        }
        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (phone.isEmpty()) {
                user.setPhone(null);
            } else if (!phone.equals(user.getPhone())) {
                if (userRepository.existsByPhoneAndIdNot(phone, userId)) {
                    throw new AppException(ErrorCode.USER_EXISTED, "Số điện thoại này đã được dùng cho tài khoản khác");
                }
                user.setPhone(phone);
            }
        }
        if (request.getAvatarUrl() != null) {
            String avatarUrl = request.getAvatarUrl().trim();
            user.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl);
        }
        return mapToProfile(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthResult changePassword(UUID userId, ChangePasswordRequest request, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        String email = normalizeEmail(user.getEmail());
        // Dùng chung bộ đếm khóa với đăng nhập: token bị lộ cũng không dò được mật khẩu hiện tại
        loginAttemptService.checkLoginAllowed(email, clientIp);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            loginAttemptService.recordLoginFailure(email, clientIp);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu hiện tại không đúng");
        }
        ensurePasswordFitsBcrypt(request.getNewPassword());
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        loginAttemptService.recordLoginSuccess(email);

        refreshTokenService.revokeAllForUser(userId);
        log.info("Người dùng {} đã đổi mật khẩu, thu hồi toàn bộ phiên cũ", userId);
        return issueTokens(user);
    }

    private AuthResult issueTokens(User user) {
        RefreshTokenService.Issued refresh = refreshTokenService.issue(user.getId());
        return new AuthResult(buildResponse(user), refresh.rawToken());
    }

    private AuthResponse buildResponse(User user) {
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.accessTokenTtlSeconds())
                .user(mapToProfile(user))
                .build();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static void ensurePasswordFitsBcrypt(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mật khẩu quá dài (tối đa 72 byte)");
        }
    }

    private UserProfileResponse mapToProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
