package org.exp.primeapp.service.impl.global.attachment;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.service.face.global.attachment.AttachmentTokenService;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentTokenServiceImpl implements AttachmentTokenService {

    private final JwtCookieService jwtCookieService;
    private final SessionService sessionService;

    @Value("${attachment.token.expiry.minutes:8}")
    private Integer tokenExpiryMinutes;

    @Override
    @Transactional
    public String generateTokenForUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required to generate attachment token");
        }

        return generateToken(user.getId(), user.getPhone());
    }

    @Override
    @Transactional
    public String generateTokenForSession(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Session is required to generate attachment token");
        }

        Long userId = session.getUser() != null ? session.getUser().getId() : null;
        String phone = session.getUser() != null ? session.getUser().getPhone() : "anonymous";

        String token = generateToken(userId, phone);
        
        // Session ga token ni biriktirish
        sessionService.setAttachmentToken(session.getSessionId(), token);
        
        return token;
    }

    private String generateToken(Long userId, String phone) {
        SecretKey secretKey = jwtCookieService.getSecretKey();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpiryMinutes * 60 * 1000L);

        String token = Jwts.builder()
                .subject("ATTACHMENT_ACCESS")
                .claim("type", "ATTACHMENT")
                .claim("userId", userId)
                .claim("phone", phone)
                .claim("uuid", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();

        return token;
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            SecretKey secretKey = jwtCookieService.getSecretKey();
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"ATTACHMENT".equals(type)) {
                log.warn("Invalid token type: {}", type);
                return false;
            }

            // Token expiry avtomatik tekshiriladi JWT parser tomonidan
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid attachment token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public String refreshToken(String oldToken, HttpServletRequest request) {
        if (!validateToken(oldToken)) {
            throw new IllegalArgumentException("Invalid token for refresh");
        }

        // Session dan token olish
        String sessionId = sessionService.getSessionIdFromCookie(request);
        if (sessionId == null) {
            throw new IllegalArgumentException("Session not found");
        }

        Session session = sessionService.getSessionById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found");
        }

        // Yangi token yaratish
        return generateTokenForSession(session);
    }

    @Override
    public long getTokenExpiryDays(String token) {
        try {
            SecretKey secretKey = jwtCookieService.getSecretKey();
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return 0;
            }

            LocalDateTime expiryDateTime = LocalDateTime.ofInstant(
                    expiration.toInstant(),
                    ZoneId.systemDefault()
            );
            LocalDateTime now = LocalDateTime.now();

            return ChronoUnit.DAYS.between(now, expiryDateTime);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to get token expiry: {}", e.getMessage());
            return 0;
        }
    }
}

