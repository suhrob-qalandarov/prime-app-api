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
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.authorizeHttpRequests(auth ->
                auth
                        // Swagger endpoints - authenticated (httpBasic will handle authentication)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui.html/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/index.html",
                                "/swagger-ui/index.html/**"
                        ).authenticated()

                        // Public auth endpoint
                        .requestMatchers(
                                HttpMethod.POST,
                                API + V2 + AUTH + "/code/*"
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

        // Swagger endpoints uchun httpBasic authentication
        http.httpBasic(httpBasic -> httpBasic.realmName("Swagger UI"));
        http.userDetailsService(swaggerUserDetailsService());

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

        List<String> allowedOrigins = new ArrayList<>();
        allowedOrigins.add(mainUrl);
        allowedOrigins.add(apiUrl);

        // Add local URLs from properties (comma-separated)
        if (localUrls != null && !localUrls.isEmpty()) {
            String[] localUrlArray = localUrls.split(",");
            for (String url : localUrlArray) {
                String trimmedUrl = url.trim();
                if (!trimmedUrl.isEmpty()) {
                    allowedOrigins.add(trimmedUrl);
                }
            }
        }

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", 
                "Content-Type", 
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Forwarded-For",
                "X-Real-IP"
        ));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
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