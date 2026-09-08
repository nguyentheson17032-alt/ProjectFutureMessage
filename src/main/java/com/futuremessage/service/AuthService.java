package com.futuremessage.service;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import com.futuremessage.config.JwtProperties;
import com.futuremessage.domain.RefreshToken;
import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.RefreshTokenRepository;
import com.futuremessage.repository.UserRepository;
import com.futuremessage.security.JwtService;
import com.futuremessage.security.TokenHasher;
import com.futuremessage.web.dto.AuthResponse;
import com.futuremessage.web.dto.LoginRequest;
import com.futuremessage.web.dto.RegisterRequest;
import com.futuremessage.web.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            MessageRepository messageRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("timing-dummy");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName().trim())
                .emailVerified(false)
                .role(UserRole.USER)
                .enabled(true)
                .build();

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Bulk tương đương Message.claimRecipient: gắn recipient_user_id khi email vừa đăng ký.
        messageRepository.linkUnclaimedMessagesToUser(user, email);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = User.normalizeEmail(request.email());
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken stored = refreshTokenRepository.findByTokenHashWithUser(TokenHasher.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.isExpired(now)) {
            stored.revoke(now);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllActiveByUserId(stored.getUser().getId(), now);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        stored.revoke(now);
        if (!stored.getUser().isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(user);
        String rawRefreshToken = generateRawRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(rawRefreshToken))
                .expiresAt(clock.instant().plus(jwtProperties.refreshTokenTtl()))
                .build();
        refreshTokenRepository.save(refreshToken);
        return AuthResponse.of(accessToken, rawRefreshToken, jwtService.accessTokenTtl(), UserResponse.from(user));
    }

    private String generateRawRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
