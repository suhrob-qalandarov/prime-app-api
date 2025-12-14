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
import org.exp.primeapp.models.entities.Session;
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

    @Value("${jwt.data.expiry.minutes:8}")
    private Integer dataExpiryMinutes;

    @Value("${jwt.counts.max.category:10}")
    private Integer maxCountCategory;

    @Value("${jwt.counts.max.product:10}")
    private Integer maxCountProduct;

    @Value("${jwt.counts.max.attachment:10}")
    private Integer maxCountAttachment;

    @Value("${jwt.counts.max.cart:10}")
    private Integer maxCountCart;

    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Transactional
    public String generateToken(User user, Session session, HttpServletRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required to generate token");
        }
        if (session == null || session.getSessionId() == null) {
            throw new IllegalArgumentException("Session is required to generate token");
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
        userClaims.put("fullName", userUtil.truncateName(user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")));
        userClaims.put("telegramId", user.getTelegramId());
        userClaims.put("roles", rolesList);

        // BrowserInfo ni olish (oxirgi qo'shilgan - eng yangi)
        String browserInfo = null;
        if (session.getBrowserInfos() != null && !session.getBrowserInfos().isEmpty()) {
            // LinkedHashSet da oxirgi element (eng yangi)
            browserInfo = session.getBrowserInfos().stream()
                    .skip(session.getBrowserInfos().size() - 1)
                    .findFirst()
                    .orElse(null);
        }

        // Build data object (counts, iat, exp)
        Date now = new Date();
        Date dataExpiry = new Date(now.getTime() + dataExpiryMinutes * 60 * 1000L);
        
        Map<String, Integer> counts = new HashMap<>();
        counts.put("category", 0);
        counts.put("product", 0);
        counts.put("attachment", 0);
        counts.put("cart", 0);
        
        Map<String, Object> dataClaims = new HashMap<>();
        dataClaims.put("iat", now.getTime() / 1000);  // Unix timestamp (seconds)
        dataClaims.put("exp", dataExpiry.getTime() / 1000);  // Unix timestamp (seconds)
        dataClaims.put("counts", counts);

        String token = Jwts.builder()
                .setSubject("SESSION")  // sub da type
                .claim("sessionId", session.getSessionId())
                .claim("ip", currentIp)
                .claim("browserInfo", browserInfo)
                .claim("isAuthenticated", session.getIsAuthenticated() != null ? session.getIsAuthenticated() : true)
                .claim("user", userClaims)
                .claim("data", dataClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiryDays * 24L * 60 * 60 * 1000)) // days from properties
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();

        // Save token to session entity
        session.setAccessToken(token);
        // Note: Session will be saved by the caller

        return token;
    }

    @Transactional
    public String generateAccessTokenForAnonymous(Session session, HttpServletRequest request) {
        if (session == null || session.getSessionId() == null) {
            throw new IllegalArgumentException("Session is required to generate token");
        }

        // Get current IP
        String currentIp = ipAddressUtil.getClientIpAddress(request);

        // BrowserInfo ni olish (oxirgi qo'shilgan - eng yangi)
        String browserInfo = null;
        if (session.getBrowserInfos() != null && !session.getBrowserInfos().isEmpty()) {
            browserInfo = session.getBrowserInfos().stream()
                    .skip(session.getBrowserInfos().size() - 1)
                    .findFirst()
                    .orElse(null);
        }

        // Build data object (counts, iat, exp)
        Date now = new Date();
        Date dataExpiry = new Date(now.getTime() + dataExpiryMinutes * 60 * 1000L);
        
        Map<String, Integer> counts = new HashMap<>();
        counts.put("category", 0);
        counts.put("product", 0);
        counts.put("attachment", 0);
        counts.put("cart", 0);
        
        Map<String, Object> dataClaims = new HashMap<>();
        dataClaims.put("iat", now.getTime() / 1000);  // Unix timestamp (seconds)
        dataClaims.put("exp", dataExpiry.getTime() / 1000);  // Unix timestamp (seconds)
        dataClaims.put("counts", counts);

        String token = Jwts.builder()
                .setSubject("SESSION")  // sub da type
                .claim("sessionId", session.getSessionId())
                .claim("ip", currentIp)
                .claim("browserInfo", browserInfo)
                .claim("isAuthenticated", false)
                .claim("user", null)
                .claim("data", dataClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiryDays * 24L * 60 * 60 * 1000)) // days from properties
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();

        // Save token to session entity
        session.setAccessToken(token);
        // Note: Session will be saved by the caller

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

            // Check token type from sub claim
            String type = claims.getSubject();
            if (!"SESSION".equals(type)) {
                log.warn("Invalid token type: {}", type);
                return false;
            }

            // Get sessionId from token
            String sessionId = claims.get("sessionId", String.class);
            if (sessionId == null) {
                log.warn("Token does not contain sessionId");
                return false;
            }

            // Get IP from token
            String tokenIp = claims.get("ip", String.class);
            if (tokenIp == null) {
                log.warn("Token does not contain IP");
                return false;
            }

            // Get request IP
            String requestIp = ipAddressUtil.getClientIpAddress(request);

            // Compare IPs
            if (!tokenIp.equals(requestIp)) {
                log.warn("IP mismatch: token IP = {}, request IP = {}", tokenIp, requestIp);
                return false;
            }

            // Check isAuthenticated (top level dan)
            Boolean isAuthenticated = claims.get("isAuthenticated", Boolean.class);
            if (isAuthenticated == null || !isAuthenticated) {
                log.warn("Session is not authenticated");
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

        // Get user claims from token
        @SuppressWarnings("unchecked")
        Map<String, Object> userClaims = (Map<String, Object>) claims.get("user");
        
        // Anonymous user uchun user null bo'lishi mumkin
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

        // Create a temporary user object with roles from token
        // We need to set roles from token claims
        @SuppressWarnings("unchecked")
        List<String> rolesList = (List<String>) userClaims.get("roles");
        if (rolesList != null) {
            // Set roles from token (we'll use the roles from database, but validate they match)
            List<String> dbRoles = user.getRoles().stream()
                    .map(Role::getAuthority)
                    .collect(Collectors.toList());
            
            // Validate roles match (optional security check)
            if (!new HashSet<>(rolesList).equals(new HashSet<>(dbRoles))) {
                log.warn("Token roles do not match database roles. Token: {}, DB: {}", rolesList, dbRoles);
                // Continue anyway, but log warning
            }
        }

        return user;
    }

    public String getSessionIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("sessionId", String.class);
    }

    public String extractTokenFromCookie(HttpServletRequest request) {
        return extractTokenFromCookie(request, cookieNameUser);
    }

    public String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            log.debug("Received {} cookies", request.getCookies().length);
            for (Cookie cookie : request.getCookies()) {
                log.debug("Cookie name: {}, value: {}", cookie.getName(), cookie.getValue() != null ? "***" : null);
                if (cookieName.equals(cookie.getName())) {
                    log.info("Found JWT cookie: {}", cookie.getName());
                    return cookie.getValue();
                }
            }
        } else {
            log.debug("No cookies in request");
        }
        return null;
    }

    public void setJwtCookie(String token, String key, HttpServletResponse response) {
        setJwtCookie(token, key, response, null);
    }

    public void setJwtCookie(String token, String key, HttpServletResponse response, HttpServletRequest request) {
        Cookie cookie = new Cookie(key, token);
        cookie.setHttpOnly(cookieIsHttpOnly);
        
        // Detect if request is to localhost
        boolean isLocalhost = false;
        if (request != null) {
            String host = request.getServerName();
            String scheme = request.getScheme();
            isLocalhost = host != null && (host.equals("localhost") || host.equals("127.0.0.1") || scheme.equals("http"));
            log.debug("Request host: {}, scheme: {}, isLocalhost: {}", host, scheme, isLocalhost);
        }
        
        // For localhost HTTP requests, set Secure=false
        // For production HTTPS, use configured Secure value
        boolean shouldBeSecure = isLocalhost ? false : cookieIsSecure;
        cookie.setSecure(shouldBeSecure);
        
        // For localhost, don't set domain (allows localhost to work)
        // For production, use configured domain
        if (!isLocalhost && cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        cookie.setPath(cookiePath);
        cookie.setMaxAge(cookieMaxAge);
        
        // For localhost, use SameSite=Lax instead of None
        // SameSite=None requires Secure=true, which doesn't work with HTTP
        if (isLocalhost) {
            cookie.setAttribute("SameSite", "Lax");
        } else {
            cookie.setAttribute(cookieAttributeName, cookieAttributeValue);
        }
        
        log.debug("Setting cookie: name={}, secure={}, domain={}, sameSite={}", 
                key, shouldBeSecure, isLocalhost ? "not set" : cookieDomain, 
                isLocalhost ? "Lax" : cookieAttributeValue);
        
        response.addCookie(cookie);
    }
}
