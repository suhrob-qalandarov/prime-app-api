package org.exp.primeapp.models.dto.responce.user;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SessionRes(
        String sessionId,
        String ip,
        String browserInfo,
        LocalDateTime expiresAt,
        Boolean isActive,
        LocalDateTime lastAccessedAt,
        LocalDateTime migratedAt
) {
}

