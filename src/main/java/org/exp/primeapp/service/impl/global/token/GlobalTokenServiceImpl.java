package org.exp.primeapp.service.impl.global.token;

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
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.service.face.global.token.GlobalTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalTokenServiceImpl implements GlobalTokenService {

    private final JwtCookieService jwtCookieService;

    @Value("${jwt.global.token.expiry.days:365}")
    private Integer globalTokenExpiryDays;

    @Value("${cookie.name.global:prime-global-token}")
    private String globalTokenCookieName;

    @Value("${cookie.domain}")
    private String cookieDomain;

    @Value("${cookie.path}")
    private String cookiePath;

    @Value("${cookie.attribute.name}")
    private String cookieAttributeName;

    @Value("${cookie.attribute.value}")
    private String cookieAttributeValue;

    @Value("${cookie.is.http.only}")
    private Boolean cookieIsHttpOnly;

    @Value("${cookie.is.secure}")
    private Boolean cookieIsSecure;

    @Override
    public String generateGlobalToken(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("SessionId is required to generate global token");
        }

        // Initialize counts
        Map<String, Integer> counts = new HashMap<>();
        counts.put("category", 0);
        counts.put("product", 0);
        counts.put("attachment", 0);
        counts.put("cart", 0);

        Date now = new Date();
        Date expiryTime = new Date(now.getTime() + 8 * 60 * 1000L); // 8 minutes
        Date validTime = now;

        SecretKey secretKey = jwtCookieService.getSecretKey();

        String token = Jwts.builder()
                .setSubject("GLOBAL_TOKEN")  // sub da type
                .claim("expiryTime", expiryTime.getTime() / 1000)  // Unix timestamp (seconds)
                .claim("validTime", validTime.getTime() / 1000)  // Unix timestamp (seconds)
                .claim("counts", counts)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + globalTokenExpiryDays * 24L * 60 * 60 * 1000)) // days from properties
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

    @Override
    public String updateGlobalTokenCounts(String token, String countType, int increment) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }

        try {
            SecretKey secretKey = jwtCookieService.getSecretKey();
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check token type from sub claim
            String type = claims.getSubject();
            if (!"GLOBAL_TOKEN".equals(type)) {
                throw new IllegalArgumentException("Invalid token type: " + type);
            }

            // Get counts
            @SuppressWarnings("unchecked")
            Map<String, Integer> counts = (Map<String, Integer>) claims.get("counts");
            if (counts == null) {
                counts = new HashMap<>();
                counts.put("category", 0);
                counts.put("product", 0);
                counts.put("attachment", 0);
                counts.put("cart", 0);
            }

            // Update count
            Integer currentCount = counts.getOrDefault(countType, 0);
            counts.put(countType, currentCount + increment);

            // Get expiry and valid times
            Long expiryTimeSeconds = claims.get("expiryTime", Long.class);
            Long validTimeSeconds = claims.get("validTime", Long.class);

            Date now = new Date();
            Date expiryTime = expiryTimeSeconds != null ? new Date(expiryTimeSeconds * 1000) : new Date(now.getTime() + 8 * 60 * 1000L);
            Date validTime = validTimeSeconds != null ? new Date(validTimeSeconds * 1000) : now;

            // Create new token with updated counts
            String newToken = Jwts.builder()
                    .setSubject("GLOBAL_TOKEN")
                    .claim("expiryTime", expiryTime.getTime() / 1000)
                    .claim("validTime", validTime.getTime() / 1000)
                    .claim("counts", counts)
                    .issuedAt(now)
                    .expiration(new Date(now.getTime() + globalTokenExpiryDays * 24L * 60 * 60 * 1000))
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();

            return newToken;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to update global token counts: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token: " + e.getMessage());
        }
    }

    @Override
    public boolean validateGlobalToken(String token) {
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

            // Check token type from sub claim
            String type = claims.getSubject();
            if (!"GLOBAL_TOKEN".equals(type)) {
                log.warn("Invalid token type: {}", type);
                return false;
            }

            // Check expiry time
            Long expiryTimeSeconds = claims.get("expiryTime", Long.class);
            if (expiryTimeSeconds != null) {
                Date expiryTime = new Date(expiryTimeSeconds * 1000);
                Date now = new Date();
                if (expiryTime.before(now)) {
                    log.warn("Global token expiry time has passed");
                    return false;
                }
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Global token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Integer> getCountsFromToken(String token) {
        if (token == null || token.isBlank()) {
            return new HashMap<>();
        }

        try {
            SecretKey secretKey = jwtCookieService.getSecretKey();
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            Map<String, Integer> counts = (Map<String, Integer>) claims.get("counts");
            if (counts == null) {
                counts = new HashMap<>();
                counts.put("category", 0);
                counts.put("product", 0);
                counts.put("attachment", 0);
                counts.put("cart", 0);
            }

            return counts;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to get counts from token: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public Long getExpiryTimeFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            SecretKey secretKey = jwtCookieService.getSecretKey();
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("expiryTime", Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to get expiry time from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Long getValidTimeFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            SecretKey secretKey = jwtCookieService.getSecretKey();
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("validTime", Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to get valid time from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractGlobalTokenFromCookie(HttpServletRequest request) {
        return jwtCookieService.extractTokenFromCookie(request, globalTokenCookieName);
    }

    @Override
    public void setGlobalTokenCookie(String token, HttpServletResponse response, HttpServletRequest request) {
        Cookie cookie = new Cookie(globalTokenCookieName, token);
        cookie.setHttpOnly(cookieIsHttpOnly);

        // Detect if request is to localhost
        boolean isLocalhost = false;
        if (request != null) {
            String host = request.getServerName();
            String scheme = request.getScheme();
            isLocalhost = host != null && (host.equals("localhost") || host.equals("127.0.0.1") || scheme.equals("http"));
        }

        // For localhost HTTP requests, set Secure=false
        boolean shouldBeSecure = isLocalhost ? false : cookieIsSecure;
        cookie.setSecure(shouldBeSecure);

        // For localhost, don't set domain
        if (!isLocalhost && cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }

        cookie.setPath(cookiePath);
        cookie.setMaxAge(globalTokenExpiryDays * 24 * 60 * 60); // days to seconds

        // For localhost, use SameSite=Lax
        if (isLocalhost) {
            cookie.setAttribute("SameSite", "Lax");
        } else {
            cookie.setAttribute(cookieAttributeName, cookieAttributeValue);
        }

        response.addCookie(cookie);
    }
}

