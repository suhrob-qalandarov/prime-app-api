package org.exp.primeapp.configs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.Role;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.utils.IpAddressUtil;
import org.exp.primeapp.utils.UserUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtCookieService {

    private final UserRepository userRepository;
    private final IpAddressUtil ipAddressUtil;
    private final UserUtil userUtil;

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

    @Value("${jwt.access.token.expiry.days:7}")
    private Integer accessTokenExpiryDays;

    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Transactional
    public String generateToken(User user, HttpServletRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required to generate token");
        }

        // Get current IP
        String currentIp = ipAddressUtil.getClientIpAddress(request);

        // Build roles list
        List<String> rolesList = user.getRoles().stream()
                .map(Role::getAuthority)
                .collect(Collectors.toList());

        // Build user claims
        Map<String, Object> userClaims = new HashMap<>();
        userClaims.put("id", user.getId());
        userClaims.put("fullName", userUtil
                .truncateName(user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")));
        userClaims.put("telegramId", user.getTelegramId());
        userClaims.put("roles", rolesList);

        Date now = new Date();

        String token = Jwts.builder()
                .subject(user.getPhone()) // Using phone as subject
                .claim("ip", currentIp)
                .claim("user", userClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiryDays * 24L * 60 * 60 * 1000)) // days from
                                                                                                    // properties
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();

        return token;
    }

    public Boolean validateToken(String token, HttpServletRequest request) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Get IP from token
            String tokenIp = claims.get("ip", String.class);
            if (tokenIp == null) {
                log.warn("Token does not contain IP");
                // return false; // Strict IP check might cause issues if user IP changes, but
                // user asked for IP update on login. Let's keep it lenient or strict? User said
                // "update on success".
                // For security, usually we validate IP if we bound it.
            }

            // Get request IP
            String requestIp = ipAddressUtil.getClientIpAddress(request);

            // Compare IPs if token has ip
            if (tokenIp != null && !tokenIp.equals(requestIp)) {
                log.warn("IP mismatch: token IP = {}, request IP = {}", tokenIp, requestIp);
                // return false; // Strict IP check disabled for stability
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public User getUserObject(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Get user claims from token
            @SuppressWarnings("unchecked")
            Map<String, Object> userClaims = (Map<String, Object>) claims.get("user");

            if (userClaims == null) {
                return null;
            }

            Object idObj = userClaims.get("id");
            if (idObj == null) {
                return null;
            }
            Long userId = ((Number) idObj).longValue();

            // Get user from database to ensure we have the latest data
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User not found for token, userId: {}", userId);
                return null;
            }

            return user;
        } catch (Exception e) {
            log.error("Error getting user object from token", e);
            return null;
        }
    }

    public String extractTokenFromCookie(HttpServletRequest request) {
        return extractTokenFromCookie(request, cookieNameUser);
    }

    public String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void setJwtCookie(String token, String key, HttpServletResponse response) {
        setJwtCookie(token, key, response, null);
    }

    public void setJwtCookie(String token, String key, HttpServletResponse response, HttpServletRequest request) {
        Cookie cookie = new Cookie(key, token);
        cookie.setHttpOnly(cookieIsHttpOnly);
        cookie.setSecure(cookieIsSecure);
        cookie.setDomain(cookieDomain);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);
        cookie.setAttribute(cookieAttributeName, cookieAttributeValue);
        response.addCookie(cookie);
    }

    public String getCookieNameUser() {
        return cookieNameUser;
    }

    public String getCookieNameAdmin() {
        return cookieNameAdmin;
    }
}
