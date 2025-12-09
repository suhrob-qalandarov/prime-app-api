package org.exp.primeapp.configs.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.utils.IpAddressUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class AuditConfig {

    private final IpAddressUtil ipAddressUtil;

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            try {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                    return Optional.of(auth.getName());
                }
                
                // anonymousUser o'rniga IP ni qaytarish
                String ip = ipAddressUtil.getClientIpAddress();
                return Optional.of(ip);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return Optional.of("SYSTEM");
        };
    }
}
