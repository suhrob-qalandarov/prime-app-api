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

    @Value("${swagger.ui.username:admin}")
    private String swaggerUsername;

    @Value("${swagger.ui.password:admin123}")
    private String swaggerPassword;

    @Value("${app.main.url:https://prime.howdy.uz}")
    private String mainUrl;

    @Value("${app.api.url:https://api.howdy.uz}")
    private String apiUrl;

    @Value("${app.local.urls}")
    private String localUrls;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http, JwtCookieFilter mySecurityFilter, IpWhitelistFilter ipWhitelistFilter) throws Exception {
        // Exclude Swagger endpoints from main filter chain (handled by swaggerSecurityFilterChain)
        http.securityMatcher(request -> {
            String path = request.getRequestURI();
            return !path.startsWith("/swagger-ui") && 
                   !path.startsWith("/v3/api-docs") && 
                   !path.equals("/swagger-ui.html");
        });
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.authorizeHttpRequests(auth ->
                auth
                        // Allow all OPTIONS requests (CORS preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // Public auth endpoints
                        .requestMatchers(
                                HttpMethod.POST,
                                API + V2 + AUTH + "/code/*"
                        ).permitAll()
                        
                        // Public session endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                API + V2 + AUTH + "/session"
                        ).permitAll()
                        
                        .requestMatchers(
                                HttpMethod.POST,
                                API + V2 + AUTH + "/session"
                        ).permitAll()

                        // Public product endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                API + V1 + PRODUCT + WAY_ALL
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                API + V1 + PRODUCTS,
                                API + V1 + PRODUCTS + BY_CATEGORY + "/*"
                        ).permitAll()

                        // Public category endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                API + V1 + CATEGORY,
                                API + V1 + CATEGORY + WAY_ALL
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                API + V1 + CATEGORIES,
                                API + V1 + CATEGORIES + WAY_ALL
                        ).permitAll()

                        // Attachment anonymous token endpoint - public
                        .requestMatchers(
                                HttpMethod.GET,
                                API + V1 + ATTACHMENT + "/token/anonymous"
                        ).permitAll()

                        // Attachment token endpoint for authenticated users
                        .requestMatchers(
                                HttpMethod.GET,
                                API + V1 + ATTACHMENT + "/token"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                API + V1 + ATTACHMENT + "/token/refresh"
                        ).permitAll() // Token bilan himoyalangan

                        // Attachment access endpoint - public (token bilan himoyalangan)
                        .requestMatchers(
                                HttpMethod.GET,
                                API + V1 + ATTACHMENT + "/*"
                        ).permitAll() // Token validation controller/service da qilinadi

                        // Allow GET requests to AdminProductController for ROLE_ADMIN and ROLE_VISITOR
                        .requestMatchers(
                                HttpMethod.GET,
                                API + V2 + ADMIN + PRODUCT,
                                API + V2 + ADMIN + PRODUCT + "/dashboard",
                                API + V2 + ADMIN + PRODUCT + "/*"
                        ).hasAnyRole("ADMIN", "VISITOR")

                        .requestMatchers(
                                HttpMethod.GET,
                                API + V2 + ADMIN + CATEGORY + WAY_ALL
                        ).hasAnyRole("ADMIN", "VISITOR")

                        .requestMatchers(
                                API + V2 + ADMIN + CATEGORY + "/**"
                        ).hasRole("ADMIN")

                        // Restrict all other AdminProductController endpoints to ROLE_ADMIN
                        .requestMatchers(
                                API + V2 + ADMIN + PRODUCT + "/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                API + V2 + ADMIN + PRODUCT + SIZE
                        ).hasAnyRole("ADMIN", "VISITOR")

                        // Allow GET requests to AdminAttachmentController for ROLE_ADMIN and ROLE_VISITOR
                        .requestMatchers(
                                HttpMethod.GET,
                                API + V1 + ADMIN + ATTACHMENT + "/{attachmentId}",
                                API + V1 + ADMIN + ATTACHMENT + "/with-url/{attachmentUrl}"
                        ).hasAnyRole("ADMIN", "VISITOR")

                        // Restrict all other AdminAttachmentController endpoints to ROLE_ADMIN
                        .requestMatchers(
                                API + V1 + ADMIN + ATTACHMENT + "/**"
                        ).hasRole("ADMIN")

                        // Setting path security
                        .requestMatchers(
                                API + V1 + ADMIN + SETTING + "/**"
                        ).hasRole("SUPER_ADMIN")

                        // Actuator health endpoint - public for health checks
                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()

                        // Actuator endpoints - only for SWE role
                        .requestMatchers(
                                "/actuator/**"
                        ).hasRole("SWE")

                        // All other requests require authentication
                        .anyRequest().authenticated()
        );

        // Add IP whitelist filter before JWT filter
        http.addFilterBefore(ipWhitelistFilter, UsernamePasswordAuthenticationFilter.class);
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
                            return host != null && (host.equals("localhost") || host.equals("127.0.0.1"));
                        }).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> httpBasic.realmName("Swagger UI"))
                .authenticationManager(swaggerAuthManager)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    // Return 401 instead of 403 for unauthenticated requests
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setHeader("WWW-Authenticate", "Basic realm=\"Swagger UI\"");
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Authentication required. Please provide Basic Auth credentials.\"}");
                }));
        return http.build();
    }
    
    @Bean
    public SecurityFilterChain apiDocsSecurityFilterChain(HttpSecurity http) throws Exception {
        // /v3/api-docs/** endpointlari public bo'lishi kerak (Swagger UI ularga murojaat qiladi)
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
        
        // Add main URL and API URL (frontend va backend)
        if (mainUrl != null && !mainUrl.isEmpty()) {
            allowedOriginPatterns.add(mainUrl);
        }
        if (apiUrl != null && !apiUrl.isEmpty()) {
            allowedOriginPatterns.add(apiUrl);
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

        // Allow all methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        
        // Allow all headers - aniq headerlar ro'yxati
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
                "Set-Cookie"
        ));
        
        // Allow credentials - frontend credentials: 'include' bilan ishlashi uchun
        configuration.setAllowCredentials(true);
        
        // Use setAllowedOriginPatterns - supports exact matches and patterns
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        
        configuration.setMaxAge(3600L); // Cache preflight requests for 1 hour
        
        // Expose headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Set-Cookie"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

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