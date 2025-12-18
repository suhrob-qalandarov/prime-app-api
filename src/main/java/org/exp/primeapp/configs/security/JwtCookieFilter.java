package org.exp.primeapp.configs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.SessionRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import static org.exp.primeapp.utils.Const.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtCookieService jwtService;
    private final SessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip JWT validation for Swagger/OpenAPI endpoints, session endpoints, and actuator health
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/swagger-ui") || 
            requestPath.startsWith("/v3/api-docs") ||
            requestPath.equals("/swagger-ui.html") ||
            requestPath.startsWith("/swagger-ui.html/") ||
            requestPath.startsWith("/actuator/health") ||
            requestPath.equals("/api/v2/auth/session") ||
            requestPath.startsWith("/api/v2/auth/code/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Public endpoints - token bo'lmasa ham o'tkazib yuborish
        boolean isPublicEndpoint = isPublicEndpoint(requestPath, request.getMethod());

        // Get token from header
        String authHeader = request.getHeader(AUTHORIZATION);
        
        // Extract token from header - supports both "Bearer <token>" and "<token>" formats
        String token = null;
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            log.debug("Authorization header found: {}", authHeader.startsWith(TOKEN_PREFIX) ? "Bearer token" : "Direct token");
            
            // Check if header starts with "Bearer " prefix
            if (authHeader.startsWith(TOKEN_PREFIX)) {
                token = authHeader.substring(TOKEN_PREFIX.length()).trim();
                log.info("Token extracted from header (Bearer format): ***");
            } else {
                // Token without Bearer prefix (for Swagger UI and direct token usage)
                // Only accept if it doesn't contain spaces (to avoid Basic auth)
                String trimmedHeader = authHeader.trim();
                if (!trimmedHeader.contains(" ") && trimmedHeader.length() > 0) {
                    token = trimmedHeader;
                    log.info("Token extracted from header (direct format): ***");
                } else {
                    log.debug("Authorization header ignored (contains spaces, likely Basic auth)");
                }
            }
        }

        // Check if token doesn't exist, get from cookie
        if (token == null) {
            log.debug("No token in header, checking cookies. Request URI: {}, Origin: {}", 
                    request.getRequestURI(), request.getHeader("Origin"));
            token = jwtService.extractTokenFromCookie(request);
            log.info("Token extracted from cookie: {}", token != null ? "***" : null);
            if (token == null && !isPublicEndpoint) {
                log.warn("No JWT token found in cookies or header for request: {} from origin: {}", 
                        request.getRequestURI(), request.getHeader("Origin"));
            }
        }

        // Public endpoint'lar uchun token bo'lmasa ham o'tkazib yuborish
        if (token == null && isPublicEndpoint) {
            log.debug("Public endpoint without token - allowing request: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        if (token != null) {
            try {
                // Step 1: Token validation (IP solishtirish)
                if (!jwtService.validateToken(token, request)) {
                    log.warn("Token validation failed");
                    if (isPublicEndpoint) {
                        send401(response, "Invalid session token");
                    } else {
                        clearCookieAndSend403(response, request, isPublicEndpoint);
                    }
                    return;
                }

                // Step 2: Get sessionId from token
                String sessionId = jwtService.getSessionIdFromToken(token);
                if (sessionId == null) {
                    log.warn("Token does not contain sessionId");
                    if (isPublicEndpoint) {
                        send401(response, "Invalid session token");
                    } else {
                        clearCookieAndSend403(response, request, isPublicEndpoint);
                    }
                    return;
                }

                // Step 3: Database dan faqat isDeleted tekshirish
                Optional<Boolean> isDeletedOpt = sessionRepository.findIsDeletedBySessionId(sessionId);
                if (isDeletedOpt.isPresent() && Boolean.TRUE.equals(isDeletedOpt.get())) {
                    log.warn("Session is deleted: {}", sessionId);
                    if (isPublicEndpoint) {
                        send401(response, "Session expired");
                    } else {
                        clearCookieAndSend403(response, request, isPublicEndpoint);
                    }
                    return;
                }

                // Step 4: Get user from token (token ichidan)
                // Anonymous user uchun user null bo'lishi mumkin
                User user = jwtService.getUserObject(token);
                if (user != null && user.getId() != null) {
                    // Step 5: Set authentication (faqat authenticated user uchun)
                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("User authenticated: {}", user.getId());
                } else {
                    log.debug("Anonymous user - no authentication set");
                }

            } catch (Exception e) {
                log.error("Token validation error: {}", e.getMessage(), e);
                if (isPublicEndpoint) {
                    send401(response, "Invalid session token");
                } else {
                    clearCookieAndSend403(response, request, isPublicEndpoint);
                }
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void clearCookieAndSend403(HttpServletResponse response, HttpServletRequest request, boolean isPublicEndpoint) {
        try {
            // Public endpoint'lar uchun cookie clear qilmaslik
            if (!isPublicEndpoint) {
                // Clear JWT cookie
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("prime-user-token", null);
                cookie.setMaxAge(0);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setSecure(true);
                response.addCookie(cookie);

                jakarta.servlet.http.Cookie cookieAdmin = new jakarta.servlet.http.Cookie("prime-admin-token", null);
                cookieAdmin.setMaxAge(0);
                cookieAdmin.setPath("/");
                cookieAdmin.setHttpOnly(true);
                cookieAdmin.setSecure(true);
                response.addCookie(cookieAdmin);
            }

            // Send response
            if (isPublicEndpoint) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid session token\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Session revoked\",\"message\":\"Your session has been revoked. Please login again.\"}");
            }
        } catch (IOException e) {
            log.error("Error sending response: {}", e.getMessage());
        }
    }

    private void send401(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
        } catch (IOException e) {
            log.error("Error sending 401: {}", e.getMessage());
        }
    }

    /**
     * Public endpoint'ni tekshirish
     */
    private boolean isPublicEndpoint(String requestPath, String method) {
        // Public GET endpoints
        if ("GET".equals(method)) {
            // Public product endpoints
            if (requestPath.startsWith("/api/v1/product") ||
                requestPath.equals("/api/v1/products") ||
                requestPath.startsWith("/api/v1/products/by-category/")) {
                return true;
            }
            
            // Public category endpoints
            if (requestPath.equals("/api/v1/category") ||
                requestPath.equals("/api/v1/category/**") ||
                requestPath.equals("/api/v1/categories") ||
                requestPath.equals("/api/v1/categories/**") ||
                requestPath.startsWith("/api/v1/category/")) {
                return true;
            }
        }
        
        // Public POST endpoints
        if ("POST".equals(method)) {
            if (requestPath.equals("/api/v2/auth/session") ||
                requestPath.startsWith("/api/v2/auth/code/")) {
                return true;
            }
        }
        
        return false;
    }
}