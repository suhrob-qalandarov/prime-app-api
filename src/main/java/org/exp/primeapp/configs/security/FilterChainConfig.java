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
import org.springframework.core.annotation.Order;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(httpBasic -> httpBasic.realmName("Swagger UI"))
                .userDetailsService(swaggerUserDetailsService());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain configure(HttpSecurity http, JwtCookieFilter mySecurityFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.authorizeHttpRequests(auth ->
                auth
                        // Swagger endpoints are handled by swaggerSecurityFilterChain
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

                        // All other requests require authentication
                        .anyRequest().authenticated()
        );

        http.addFilterBefore(mySecurityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService swaggerUserDetailsService() {
        UserDetails swaggerUser = User.builder()
                .username(swaggerUsername)
                .password(passwordEncoder().encode(swaggerPassword))
                .roles("SWAGGER")
                .build();
        return new InMemoryUserDetailsManager(swaggerUser);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "https://prime.howdy.uz", "https://api.howdy.uz",  "https://admin.howdy.uz",
                "https://prime77.uz",  "https://api.prime77.uz", "https://admin.prime77.uz",
                "http://localhost:3000", "http://localhost:3001",
                "http://192.168.1.2:3000", "http://192.168.1.2"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));

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