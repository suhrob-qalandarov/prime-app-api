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
import static org.exp.primeapp.utils.Const.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtCookieService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip JWT validation for Swagger/OpenAPI endpoints (Actuator endpoints require SWE role via Spring Security)
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/swagger-ui") || 
            requestPath.startsWith("/v3/api-docs") ||
            requestPath.equals("/swagger-ui.html") ||
            requestPath.startsWith("/swagger-ui.html/") ||
            requestPath.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get token from header
        String authHeader = request.getHeader(AUTHORIZATION);
        
        // Extract token from header - supports both "Bearer <token>" and "<token>" formats
        String token = null;
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            // Skip logging for actuator health endpoint to reduce log noise
            if (!requestPath.startsWith("/actuator/health")) {
                log.debug("Authorization header found: {}", authHeader.startsWith(TOKEN_PREFIX) ? "Bearer token" : "Direct token");
            }
            
            // Check if header starts with "Bearer " prefix
            if (authHeader.startsWith(TOKEN_PREFIX)) {
                token = authHeader.substring(TOKEN_PREFIX.length()).trim();
                if (!requestPath.startsWith("/actuator/health")) {
                    log.info("Token extracted from header (Bearer format): ***");
                }
            } else {
                // Token without Bearer prefix (for Swagger UI and direct token usage)
                // Only accept if it doesn't contain spaces (to avoid Basic auth)
                String trimmedHeader = authHeader.trim();
                if (!trimmedHeader.contains(" ") && trimmedHeader.length() > 0) {
                    token = trimmedHeader;
                    if (!requestPath.startsWith("/actuator/health")) {
                        log.info("Token extracted from header (direct format): ***");
                    }
                } else {
                    if (!requestPath.startsWith("/actuator/health")) {
                        log.debug("Authorization header ignored (contains spaces, likely Basic auth)");
                    }
                }
            }
        }

        // Check if token doesn't exist, get from cookie
        if (token == null) {
            log.debug("No token in header, checking cookies. Request URI: {}, Origin: {}", 
                    request.getRequestURI(), request.getHeader("Origin"));
            token = jwtService.extractTokenFromCookie(request);
            // Skip logging for actuator health endpoint to reduce log noise
            if (!requestPath.startsWith("/actuator/health")) {
                log.info("Token extracted from cookie: {}", token != null ? "***" : null);
                if (token == null) {
                    log.warn("No JWT token found in cookies or header for request: {} from origin: {}", 
                            request.getRequestURI(), request.getHeader("Origin"));
                }
            }
        }

        if (token != null) {
            try {
                if (jwtService.validateToken(token)) {
                    User user = jwtService.getUserObject(token);
                    log.info("Validated user: {}", user);

                    if (user == null || user.getId() == null) {
                        log.error("User or ID is null from token: {}", user);
                        response.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
                        response.getWriter().write("Invalid user data from token");
                        throw new IllegalArgumentException("Invalid user data from token");
                    }

                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.error("Invalid token: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}