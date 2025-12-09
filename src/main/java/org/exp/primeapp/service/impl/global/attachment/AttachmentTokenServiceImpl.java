package org.exp.primeapp.service.impl.global.attachment;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.service.face.global.attachment.AttachmentTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentTokenServiceImpl implements AttachmentTokenService {

    private final JwtCookieService jwtCookieService;

    @Value("${attachment.token.expiry.minutes:15}")
    private Integer tokenExpiryMinutes;

    @Override
    public String generateToken() {
        SecretKey secretKey = jwtCookieService.getSecretKey();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpiryMinutes * 60 * 1000L);

        return Jwts.builder()
                .setSubject("ATTACHMENT_ACCESS")
                .claim("type", "ATTACHMENT")
                .claim("uuid", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            SecretKey secretKey = jwtCookieService.getSecretKey();
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            return "ATTACHMENT".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid attachment token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String refreshToken(String oldToken) {
        if (!validateToken(oldToken)) {
            throw new IllegalArgumentException("Invalid token for refresh");
        }
        return generateToken();
    }
}

