package org.exp.primeapp.configs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.Role;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtCookieService {

    private final UserRepository userRepository;

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Value("${cookie.domain}")
    private String cookieDomain;

    @Value("${cookie.path}")
    private String cookiePath;

    @Value("${cookie.attribute.name}")
    private String cookieAttributeName;

    @Value("${cookie.attribute.value}")
    private String cookieAttributeValue;

    @Value("${cookie.max.age}")
    private Integer cookieMaxAge;

    @Value("${cookie.is.http.only}")
    private Boolean cookieIsHttpOnly;

    @Value("${cookie.is.secure}")
    private Boolean cookieIsSecure;

    @Value("${cookie.name.user}")
    private String cookieNameUser;

    @Value("${cookie.name.admin}")
    private String cookieNameAdmin;

    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Transactional
    public String generateToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required to generate token");
        }

        String token = Jwts.builder()
                .setSubject(user.getPhone())
                .claim("id", user.getId())
                .claim("firstName", user.getFirstName())
                .claim("phoneNumber", user.getPhone())
                //.claim("active", user.getActive())
                .claim("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")))
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 1000 * 60 * 60 * 24 * 7))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();

        // Save token to user entity
        user.setAccessToken(token);
        userRepository.save(user);

        return token;
    }

    public Boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("id", Long.class);
            if (userId == null) {
                log.warn("Token does not contain user ID");
                return false;
            }

            // Get user from database and check if token matches
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User not found for token validation");
                return false;
            }

            // Check if token matches the one stored in user entity
            if (!token.equals(user.getAccessToken())) {
                log.warn("Token does not match user's stored token");
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public User getUserObject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long id = claims.get("id", Long.class);
        if (id == null) {
            throw new IllegalArgumentException("Token does not contain user ID");
        }

        // Get user from database to ensure we have the latest data
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found for token");
        }

        // Verify token matches the one stored in user entity
        if (!token.equals(user.getAccessToken())) {
            throw new IllegalArgumentException("Token does not match user's stored token");
        }

        return user;
    }

    public String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieNameUser.equals(cookie.getName())
                        || cookieNameAdmin.equals(cookie.getName())) {

                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void setJwtCookie(String token, String key, HttpServletResponse response) {
        Cookie cookie = new Cookie(key, token);
        cookie.setHttpOnly(cookieIsHttpOnly);
        cookie.setSecure(cookieIsSecure);
        cookie.setDomain(cookieDomain);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);
        cookie.setAttribute(cookieAttributeName, cookieAttributeValue);
        response.addCookie(cookie);
    }
}
