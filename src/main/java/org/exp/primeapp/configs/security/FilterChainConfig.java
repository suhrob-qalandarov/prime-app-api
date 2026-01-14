package org.exp.primeapp.configs.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.exp.primeapp.utils.Const.*;

@EnableMethodSecurity
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class FilterChainConfig {

        private final CustomUserDetailsService customUserDetailsService;

        @Value("${swagger.ui.username}")
        private String swaggerUsername;

        @Value("${swagger.ui.password}")
        private String swaggerPassword;

        @Value("${app.main.url}")
        private String mainUrl;

        @Value("${app.api.url}")
        private String apiUrl;

        @Value("${app.local.urls}")
        private String localUrls;

        @Bean
        public SecurityFilterChain configure(HttpSecurity http, JwtCookieFilter mySecurityFilter) throws Exception {
                // Exclude Swagger endpoints from main filter chain (handled by swaggerSecurityFilterChain)
                http.securityMatcher(request -> {
                        String path = request.getRequestURI();
                        return !path.startsWith("/swagger-ui") &&
                                !path.startsWith("/v3/api-docs") &&
                                !path.equals("/swagger-ui.html");
                });

                http.csrf(AbstractHttpConfigurer::disable);
                http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
                http.authorizeHttpRequests(auth -> auth
                                // Allow all OPTIONS requests (CORS preflight)
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public auth endpoint
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v2/auth/code/*"
                        ).permitAll()

                        // Public attachment endpoint
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/attachment/*"
                        ).permitAll()

                        // Public uploads endpoint
                        .requestMatchers(
                                HttpMethod.GET,
                                "/uploads/**"
                        ).permitAll()

                        // Public product endpoint
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/product/*"
                        ).permitAll()

                        // Public product endpoint
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/category/*"
                        ).permitAll()

                        // Public cart endpoint
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/cart"
                        ).permitAll()

                        // Restricted admin product endpoint
                        .requestMatchers(
                                "/api/v2/admin/product/**"
                        ).hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        // Restricted admin product size endpoint
                        .requestMatchers(
                                "/api/v2/admin/product/size",
                                "/api/v2/admin/product/size/**"
                        ).hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        // Restricted admin income endpoint
                        .requestMatchers(
                                "/api/v1/admin/inventory-transactions/**"
                        ).hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        // Restricted admin category endpoint
                        .requestMatchers(
                                "/api/v2/admin/category/**"
                        ).hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        // Restricted admin order endpoint
                        .requestMatchers(
                                "/api/v1/admin/order/**"
                        ).hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        // Restricted admin attachment endpoint
                        .requestMatchers(
                                "/api/v1/admin/attachment/**"
                        ).hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        // Actuator health endpoint public for health checks on deployment
                        .requestMatchers(
                                "/actuator/health")
                        .permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated());

                // JWT filter
                http.addFilterBefore(mySecurityFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
                // Create authentication provider for Swagger Basic Auth
                var swaggerAuthProvider = new DaoAuthenticationProvider();
                swaggerAuthProvider.setPasswordEncoder(passwordEncoder());
                swaggerAuthProvider.setUserDetailsService(swaggerUserDetailsService());
                var swaggerAuthManager = new ProviderManager(swaggerAuthProvider);

                http.securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/swagger-ui.html/**",
                                "/swagger-ui/index.html", "/swagger-ui/index.html/**")
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                // Localhost uchun public, production uchun authenticated
                                                .requestMatchers(request -> {
                                                        String host = request.getServerName();
                                                        return host != null && (host.equals("localhost")
                                                                        || host.equals("127.0.0.1"));
                                                }).permitAll()
                                                .anyRequest().authenticated())
                                .httpBasic(httpBasic -> httpBasic.realmName("Swagger UI"))
                                .authenticationManager(swaggerAuthManager)
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        // Return 401 instead of 403 for unauthenticated requests
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setHeader("WWW-Authenticate",
                                                                        "Basic realm=\"Swagger UI\"");
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                        "{\"error\":\"Authentication required. Please provide Basic Auth credentials.\"}");
                                                }));
                return http.build();
        }

        @Bean
        public SecurityFilterChain apiDocsSecurityFilterChain(HttpSecurity http) throws Exception {
                // /v3/api-docs/** endpointlari public bo'lishi kerak (Swagger UI ularga
                // murojaat qiladi)
                http.securityMatcher("/v3/api-docs/**")
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
                return http.build();
        }

        @Bean
        public UserDetailsService swaggerUserDetailsService() {
                UserDetails swaggerUser = User.builder()
                                .username(swaggerUsername)
                                .password(passwordEncoder().encode(swaggerPassword))
                                .roles("SWE")
                                .build();
                return new InMemoryUserDetailsManager(swaggerUser);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                List<String> allowedOriginPatterns = new ArrayList<>();

                // Add main URL and API URL (frontend va backend) - trailing slash bilan va
                // without
                if (mainUrl != null && !mainUrl.isEmpty()) {
                        String cleanMainUrl = mainUrl.endsWith("/") ? mainUrl.substring(0, mainUrl.length() - 1)
                                        : mainUrl;
                        allowedOriginPatterns.add(cleanMainUrl);
                }
                if (apiUrl != null && !apiUrl.isEmpty()) {
                        String cleanApiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
                        allowedOriginPatterns.add(cleanApiUrl);
                }

                // Add local URLs from properties (comma-separated)
                if (localUrls != null && !localUrls.isEmpty()) {
                        String[] localUrlArray = localUrls.split(",");
                        for (String url : localUrlArray) {
                                String trimmedUrl = url.trim();
                                if (!trimmedUrl.isEmpty() && !allowedOriginPatterns.contains(trimmedUrl)) {
                                        allowedOriginPatterns.add(trimmedUrl);
                                }
                        }
                }

                // Debug log
                System.out.println("=== CORS Configuration ===");
                System.out.println("Allowed Origins: " + allowedOriginPatterns);

                // Allow all methods
                configuration.setAllowedMethods(
                                Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

                // Allow all headers - aniq headerlar ro'yxati (wildcard * ishlamaydi)
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Requested-With",
                                "Accept",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers",
                                "X-Forwarded-For",
                                "X-Real-IP",
                                "Accept-Language",
                                "Cache-Control",
                                "Pragma",
                                "Cookie",
                                "Set-Cookie"));

                // Allow credentials - frontend credentials: 'include' bilan ishlashi uchun
                // IMPORTANT: Wildcard * bilan allowCredentials true ishlamaydi
                configuration.setAllowCredentials(true);

                // Set allowed origin patterns
                configuration.setAllowedOriginPatterns(allowedOriginPatterns);

                configuration.setMaxAge(3600L); // Cache preflight requests for 1 hour

                // Expose headers
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Access-Control-Allow-Origin",
                                "Access-Control-Allow-Credentials",
                                "Set-Cookie"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                System.out.println("CORS configured for all paths");
                System.out.println("============================");

                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                var authProvider = new DaoAuthenticationProvider();
                authProvider.setPasswordEncoder(passwordEncoder());
                authProvider.setUserDetailsService(customUserDetailsService);
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager() {
                return new ProviderManager(authenticationProvider());
        }
}