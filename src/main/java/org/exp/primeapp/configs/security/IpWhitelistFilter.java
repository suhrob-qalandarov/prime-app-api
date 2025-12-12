package org.exp.primeapp.configs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.utils.IpAddressUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpWhitelistFilter extends OncePerRequestFilter {

    private final IpAddressUtil ipAddressUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${ip.whitelist.enabled:false}")
    private boolean whitelistEnabled;

    @Value("${ip.whitelist:}")
    private String whitelistIps;

    // Paths that should be checked for IP whitelist
    private static final List<String> PROTECTED_PATHS = Arrays.asList(
            // User auth endpoints
            API + V2 + AUTH + "/**",
            // User endpoints
            API + V1 + USER + "/**",
            // Public endpoints that need IP restriction
            API + V1 + PRODUCT + "/**",
            API + V1 + PRODUCTS + "/**",
            API + V1 + CATEGORY + "/**",
            API + V1 + CATEGORIES + "/**",
            API + V1 + ATTACHMENT + "/**"
    );

    // Paths to exclude from IP whitelist check (admin endpoints, swagger, actuator)
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            API + V1 + ADMIN + "/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui.html/**",
            "/actuator/**"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // If whitelist is disabled, skip check
        if (!whitelistEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();

        // Check if path should be excluded (admin endpoints)
        boolean isExcluded = EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));

        if (isExcluded) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if path matches protected paths
        boolean isProtected = PROTECTED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));

        if (!isProtected) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get client IP address
        String clientIp = ipAddressUtil.getClientIpAddress(request);
        log.debug("Checking IP whitelist for path: {}, IP: {}", requestPath, clientIp);

        // Parse whitelist IPs
        List<String> allowedIps = parseWhitelistIps();

        // Check if IP is in whitelist
        if (allowedIps.isEmpty()) {
            log.warn("IP whitelist is enabled but no IPs configured. Allowing request from: {}", clientIp);
            filterChain.doFilter(request, response);
            return;
        }

        boolean isAllowed = allowedIps.stream()
                .anyMatch(ip -> ip.equals(clientIp) || ip.equals("*") || matchesSubnet(clientIp, ip));

        if (!isAllowed) {
            log.warn("IP {} is not in whitelist. Blocking request to: {}", clientIp, requestPath);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"your ip address is not whitelisted." + clientIp + "\"}");
            return;
        }

        log.debug("IP {} is in whitelist. Allowing request to: {}", clientIp, requestPath);
        filterChain.doFilter(request, response);
    }

    private List<String> parseWhitelistIps() {
        if (whitelistIps == null || whitelistIps.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(whitelistIps.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean matchesSubnet(String clientIp, String whitelistIp) {
        // Simple subnet matching for CIDR notation (e.g., 192.168.1.0/24)
        if (!whitelistIp.contains("/")) {
            return false;
        }

        try {
            String[] parts = whitelistIp.split("/");
            String networkIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            String[] clientParts = clientIp.split("\\.");
            String[] networkParts = networkIp.split("\\.");

            if (clientParts.length != 4 || networkParts.length != 4) {
                return false;
            }

            int mask = 0xFFFFFFFF << (32 - prefixLength);
            int clientInt = (Integer.parseInt(clientParts[0]) << 24) |
                           (Integer.parseInt(clientParts[1]) << 16) |
                           (Integer.parseInt(clientParts[2]) << 8) |
                           Integer.parseInt(clientParts[3]);
            int networkInt = (Integer.parseInt(networkParts[0]) << 24) |
                            (Integer.parseInt(networkParts[1]) << 16) |
                            (Integer.parseInt(networkParts[2]) << 8) |
                            Integer.parseInt(networkParts[3]);

            return (clientInt & mask) == (networkInt & mask);
        } catch (Exception e) {
            log.error("Error matching subnet for IP: {} with pattern: {}", clientIp, whitelistIp, e);
            return false;
        }
    }
}

