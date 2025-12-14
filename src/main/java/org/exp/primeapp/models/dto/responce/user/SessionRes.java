package org.exp.primeapp.models.dto.responce.user;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SessionRes(
        String sessionId,
        String ip,
        String browserInfo,
        Boolean isActive,
        Boolean isDeleted,
        Boolean isAuthenticated,
        Boolean isMainSession,
        LocalDateTime lastAccessedAt,
        LocalDateTime migratedAt
) {
}

