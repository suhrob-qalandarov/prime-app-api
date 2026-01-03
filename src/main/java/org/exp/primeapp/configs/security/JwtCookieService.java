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
        userClaims.put("fullName", userUtil
                .truncateName(user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")));
        userClaims.put("telegramId", user.getTelegramId());
        userClaims.put("roles", rolesList);

        String browserInfo = session.getBrowserInfo();

        // Build data object (counts, iat, exp)
        Date now = new Date();
        Date dataExpiry = new Date(now.getTime() + dataExpiryMinutes * 60 * 1000L);

        Map<String, Integer> counts = new HashMap<>();
        counts.put("category", 0);
        counts.put("product", 0);
        counts.put("attachment", 0);
        counts.put("cart", 0);

        Map<String, Object> dataClaims = new HashMap<>();
        dataClaims.put("iat", now.getTime() / 1000); // Unix timestamp (seconds)
        dataClaims.put("exp", dataExpiry.getTime() / 1000); // Unix timestamp (seconds)
        dataClaims.put("counts", counts);

        String token = Jwts.builder()
                .subject("SESSION") // sub da type
                .claim("sessionId", session.getSessionId())
                .claim("ip", currentIp)
                .claim("browserInfo", browserInfo)
                .claim("isAuthenticated", session.getIsAuthenticated() != null ? session.getIsAuthenticated() : true)
                .claim("user", userClaims)
                .claim("data", dataClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiryDays * 24L * 60 * 60 * 1000)) // days from
                                                                                                    // properties
                .signWith(getSecretKey(), Jwts.SIG.HS256)
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

        String browserInfo = session.getBrowserInfo();

        // Build data object (counts, iat, exp)
        Date now = new Date();
        Date dataExpiry = new Date(now.getTime() + dataExpiryMinutes * 60 * 1000L);

        // Yangi session yaratilganda max count qiymatlarini o'rnatish
        Map<String, Integer> counts = new HashMap<>();
        counts.put("category", maxCountCategory);
        counts.put("product", maxCountProduct);
        counts.put("attachment", maxCountAttachment);
        counts.put("cart", maxCountCart);

        Map<String, Object> dataClaims = new HashMap<>();
        dataClaims.put("iat", now.getTime() / 1000); // Unix timestamp (seconds)
        dataClaims.put("exp", dataExpiry.getTime() / 1000); // Unix timestamp (seconds)
        dataClaims.put("counts", counts);

        String token = Jwts.builder()
                .subject("SESSION") // sub da type
                .claim("sessionId", session.getSessionId())
                .claim("ip", currentIp)
                .claim("browserInfo", browserInfo)
                .claim("isAuthenticated", false)
                .claim("user", null)
                .claim("data", dataClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiryDays * 24L * 60 * 60 * 1000)) // days from
                                                                                                    // properties
                .signWith(getSecretKey(), Jwts.SIG.HS256)
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

            // isAuthenticated tekshirish olib tashlandi - anonymous session'lar uchun false
            // bo'lishi mumkin
            // Faqat token validligini tekshiramiz (IP, sessionId, type)

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
            // Set roles from token (we'll use the roles from database, but validate they
            // match)
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

    public String getIpFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("ip", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract IP from token: {}", e.getMessage());
            return null;
        }
    }

    public String getBrowserInfoFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("browserInfo", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract browserInfo from token: {}", e.getMessage());
            return null;
        }
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

    /**
     * Token'dan product count olish
     */
    public Integer getProductCount(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            Map<String, Object> dataClaims = (Map<String, Object>) claims.get("data");
            if (dataClaims == null) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            Map<String, Integer> counts = (Map<String, Integer>) dataClaims.get("counts");
            if (counts == null) {
                return 0;
            }

            return counts.getOrDefault("product", 0);
        } catch (Exception e) {
            log.warn("Failed to get product count from token: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Token'dan category count olish
     */
    public Integer getCategoryCount(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            Map<String, Object> dataClaims = (Map<String, Object>) claims.get("data");
            if (dataClaims == null) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            Map<String, Integer> counts = (Map<String, Integer>) dataClaims.get("counts");
            if (counts == null) {
                return 0;
            }

            return counts.getOrDefault("category", 0);
        } catch (Exception e) {
            log.warn("Failed to get category count from token: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Token'dan attachment count olish
     */
    public Integer getAttachmentCount(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            Map<String, Object> dataClaims = (Map<String, Object>) claims.get("data");
            if (dataClaims == null) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            Map<String, Integer> counts = (Map<String, Integer>) dataClaims.get("counts");
            if (counts == null) {
                return 0;
            }

            return counts.getOrDefault("attachment", 0);
        } catch (Exception e) {
            log.warn("Failed to get attachment count from token: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Token'dan cart count olish
     */
    public Integer getCartCount(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            Map<String, Object> dataClaims = (Map<String, Object>) claims.get("data");
            if (dataClaims == null) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            Map<String, Integer> counts = (Map<String, Integer>) dataClaims.get("counts");
            if (counts == null) {
                return 0;
            }

            return counts.getOrDefault("cart", 0);
        } catch (Exception e) {
            log.warn("Failed to get cart count from token: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Data expiry tekshirish (data.exp tekshiriladi)
     */
    public boolean isDataExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            Map<String, Object> dataClaims = (Map<String, Object>) claims.get("data");
            if (dataClaims == null) {
                return true;
            }

            Object expObj = dataClaims.get("exp");
            if (expObj == null) {
                return true;
            }

            Long exp = ((Number) expObj).longValue();
            long currentTime = System.currentTimeMillis() / 1000;

            return exp < currentTime;
        } catch (Exception e) {
            log.warn("Failed to check data expiry: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Count'ni kamaytirib token yangilash - faqat count o'zgaradi, boshqa
     * ma'lumotlar o'zgarishmaydi
     */
    @Transactional
    public String updateTokenWithDecrementedCount(String token, String countType, HttpServletRequest request,
            Session session) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Eski ma'lumotlarni olish (barcha ma'lumotlar o'zgarishmasligi kerak)
            String sessionId = claims.get("sessionId", String.class);
            String ip = claims.get("ip", String.class);
            String browserInfo = claims.get("browserInfo", String.class);
            Boolean isAuthenticated = claims.get("isAuthenticated", Boolean.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> userClaims = (Map<String, Object>) claims.get("user");

            // Eski issuedAt va expiration ni olish
            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();

            // Data claims ni olish va faqat count ni yangilash
            @SuppressWarnings("unchecked")
            Map<String, Object> dataClaims = (Map<String, Object>) claims.get("data");
            if (dataClaims == null) {
                dataClaims = new HashMap<>();
                // Agar data yo'q bo'lsa, default qiymatlar qo'shish
                Date now = new Date();
                dataClaims.put("iat", now.getTime() / 1000);
                dataClaims.put("exp", new Date(now.getTime() + dataExpiryMinutes * 60 * 1000L).getTime() / 1000);
            }

            // Eski iat va exp ni saqlash
            Object iat = dataClaims.get("iat");
            Object exp = dataClaims.get("exp");

            @SuppressWarnings("unchecked")
            Map<String, Integer> counts = (Map<String, Integer>) dataClaims.get("counts");
            if (counts == null) {
                counts = new HashMap<>();
            }

            // Faqat count'ni kamaytirish
            int currentCount = counts.getOrDefault(countType, 0);
            counts.put(countType, Math.max(0, currentCount - 1));

            // Data claims ni yangilash - faqat counts o'zgaradi, iat va exp o'zgarishmaydi
            Map<String, Object> newDataClaims = new HashMap<>();
            newDataClaims.put("iat", iat);
            newDataClaims.put("exp", exp);
            newDataClaims.put("counts", counts);

            // Yangi token yaratish - barcha eski ma'lumotlar bilan
            String newToken = Jwts.builder()
                    .setSubject("SESSION")
                    .claim("sessionId", sessionId)
                    .claim("ip", ip)
                    .claim("browserInfo", browserInfo)
                    .claim("isAuthenticated", isAuthenticated)
                    .claim("user", userClaims)
                    .claim("data", newDataClaims)
                    .issuedAt(issuedAt != null ? issuedAt : new Date())
                    .expiration(expiration != null ? expiration
                            : new Date(System.currentTimeMillis() + accessTokenExpiryDays * 24L * 60 * 60 * 1000))
                    .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                    .compact();

            // Session'ga saqlash
            if (session != null) {
                session.setAccessToken(newToken);
            }

            return newToken;
        } catch (Exception e) {
            log.error("Failed to update token with decremented count: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update token", e);
        }
    }

    /**
     * Yangi token yaratish - expiry bugan bo'lsa, har bir count type uchun max
     * count qiymatlarini application.properties'dan oladi
     */
    @Transactional
    public String generateNewTokenWithCount(String countType, int count, Session session, HttpServletRequest request) {
        User user = session.getUser();

        // Get current IP
        String currentIp = ipAddressUtil.getClientIpAddress(request);
        String browserInfo = session.getBrowserInfo();

        // Build user claims
        Map<String, Object> userClaims = null;
        List<String> rolesList = null;
        if (user != null && user.getId() != null) {
            rolesList = user.getRoles().stream()
                    .map(Role::getAuthority)
                    .collect(Collectors.toList());

            userClaims = new HashMap<>();
            userClaims.put("id", user.getId());
            userClaims.put("fullName", userUtil
                    .truncateName(user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")));
            userClaims.put("telegramId", user.getTelegramId());
            userClaims.put("roles", rolesList);
        }

        // Build data object (counts, iat, exp)
        Date now = new Date();
        Date dataExpiry = new Date(now.getTime() + dataExpiryMinutes * 60 * 1000L);

        // Har bir count type uchun max count qiymatlarini application.properties'dan
        // olish
        Map<String, Integer> counts = new HashMap<>();
        if (count == -1) {
            // Expiry bugan bo'lsa, har bir count type uchun max count qiymatlarini olish
            counts.put("category", maxCountCategory);
            counts.put("product", maxCountProduct);
            counts.put("attachment", maxCountAttachment);
            counts.put("cart", maxCountCart);
        } else {
            // Agar count berilgan bo'lsa, faqat o'sha count type uchun count ni qo'yish
            counts.put("category", countType.equals("category") ? count : 0);
            counts.put("product", countType.equals("product") ? count : 0);
            counts.put("attachment", countType.equals("attachment") ? count : 0);
            counts.put("cart", countType.equals("cart") ? count : 0);
        }

        Map<String, Object> dataClaims = new HashMap<>();
        dataClaims.put("iat", now.getTime() / 1000);
        dataClaims.put("exp", dataExpiry.getTime() / 1000);
        dataClaims.put("counts", counts);

        String newToken = Jwts.builder()
                .setSubject("SESSION")
                .claim("sessionId", session.getSessionId())
                .claim("ip", currentIp)
                .claim("browserInfo", browserInfo)
                .claim("isAuthenticated", session.getIsAuthenticated() != null ? session.getIsAuthenticated() : false)
                .claim("user", userClaims)
                .claim("data", dataClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiryDays * 24L * 60 * 60 * 1000))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();

        // Session'ga saqlash
        session.setAccessToken(newToken);

        return newToken;
    }
}
