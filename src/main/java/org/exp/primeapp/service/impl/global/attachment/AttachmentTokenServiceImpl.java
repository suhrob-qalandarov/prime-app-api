package org.exp.primeapp.service.impl.global.attachment;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.global.attachment.AttachmentTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentTokenServiceImpl implements AttachmentTokenService {

    private final JwtCookieService jwtCookieService;
    private final UserRepository userRepository;

    @Value("${attachment.token.expiry.minutes:15}")
    private Integer tokenExpiryMinutes;

    @Override
    @Transactional
    public String generateToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required to generate attachment token");
        }

        SecretKey secretKey = jwtCookieService.getSecretKey();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpiryMinutes * 60 * 1000L);

        String token = Jwts.builder()
                .setSubject("ATTACHMENT_ACCESS")
                .claim("type", "ATTACHMENT")
                .claim("userId", user.getId())
                .claim("uuid", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // Save token to user entity
        user.setAttachmentToken(token);
        userRepository.save(user);

        return token;
    }

    @Override
    public boolean validateToken(String token, User user) {
        if (token == null || user == null || user.getId() == null) {
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
            Long userId = claims.get("userId", Long.class);

            // Check token type and user ID match
            if (!"ATTACHMENT".equals(type) || !user.getId().equals(userId)) {
                log.warn("Token type or user ID mismatch");
                return false;
            }

            // Check if token matches the one stored in user entity
            if (!token.equals(user.getAttachmentToken())) {
                log.warn("Token does not match user's stored token");
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid attachment token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public String refreshToken(String oldToken, User user) {
        if (!validateToken(oldToken, user)) {
            throw new IllegalArgumentException("Invalid token for refresh");
        }
        return generateToken(user);
    }
}

