package org.exp.primeapp.configs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.exp.primeapp.utils.Const.AUTHORIZATION;
import static org.exp.primeapp.utils.Const.TOKEN_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtCookieService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Skip JWT validation for whitelisted endpoints
        if (requestPath.startsWith("/swagger-ui") ||
                requestPath.startsWith("/v3/api-docs") ||
                requestPath.equals("/swagger-ui.html") ||
                requestPath.startsWith("/swagger-ui.html/") ||
                requestPath.startsWith("/actuator/health") ||
                requestPath.startsWith("/api/v2/auth/code/") ||
                requestPath.equals("/api/v1/admin/auth") ||
                requestPath.equals("/api/v2/admin/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isPublicEndpoint = isPublicEndpoint(requestPath, request.getMethod());
        boolean isAdminEndpoint = requestPath.startsWith("/api/v1/admin") ||
                requestPath.startsWith("/api/v2/admin");

        String token = extractToken(request);

        if (token != null) {
            try {
                if (jwtService.validateToken(token, request)) {
                    User user = jwtService.getUserObject(token);
                    if (user != null && user.getId() != null) {
                        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.debug("User authenticated: {}", userUtilTruncate(user.getFirstName()));
                    }
                } else {
                    log.warn("Token validation failed");
                    if (!isPublicEndpoint) {
                        clearCookieAndSend403(response, isPublicEndpoint);
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("Token validation error: {}", e.getMessage());
                if (!isPublicEndpoint) {
                    clearCookieAndSend403(response, isPublicEndpoint);
                    return;
                }
            }
        } else {
            // No token found
            if (!isPublicEndpoint && !isAdminEndpoint) {
                // Logic to block is below
            }
        }

        if (token == null && !isPublicEndpoint) {
            if (isAdminEndpoint) {
                filterChain.doFilter(request, response);
                return;
            }

            // Token missing -> 401 Unauthorized
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            if (authHeader.startsWith(TOKEN_PREFIX)) {
                return authHeader.substring(TOKEN_PREFIX.length()).trim();
            } else {
                String trimmed = authHeader.trim();
                if (!trimmed.contains(" ") && trimmed.length() > 0) {
                    return trimmed;
                }
            }
        }
        return jwtService.extractTokenFromCookie(request);
    }

    private void clearCookieAndSend403(HttpServletResponse response, boolean isPublicEndpoint) {
        try {
            if (!isPublicEndpoint) {
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

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Session revoked\",\"message\":\"Your session has been revoked. Please login again.\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid token\"}");
            }

        } catch (IOException e) {
            log.error("Error sending response: {}", e.getMessage());
        }
    }

    private boolean isPublicEndpoint(String requestPath, String method) {
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Public GET endpoints
        if ("GET".equals(method)) {
            return requestPath.startsWith("/api/v1/product") ||
                    requestPath.equals("/api/v1/products") ||
                    requestPath.startsWith("/api/v1/products/by-category/") ||
                    requestPath.contains("/api/v1/attachment") ||
                    requestPath.startsWith("/api/v1/cart") ||
                    requestPath.startsWith("/uploads/") ||
                    requestPath.equals("/api/v1/category") ||
                    requestPath.startsWith("/api/v1/category/") ||
                    requestPath.equals("/api/v1/categories") ||
                    requestPath.startsWith("/api/v1/categories/");
        }

        // Public POST endpoints
        if ("POST".equals(method)) {
            return requestPath.startsWith("/api/v2/auth/code/") ||
                    requestPath.equals("/api/v1/cart"); // Cart is public
        }

        return false;
    }

    private String userUtilTruncate(String name) {
        if (name == null)
            return "";
        return name.length() > 10 ? name.substring(0, 10) + "..." : name;
    }
}