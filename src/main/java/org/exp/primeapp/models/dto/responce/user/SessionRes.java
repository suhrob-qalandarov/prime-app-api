package org.exp.primeapp.models.dto.responce.user;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SessionRes(
        String sessionId,
        String ip,
        java.util.List<String> browserInfos,
        Boolean isActive,
        Boolean isDeleted,
        Boolean isAuthenticated,
        Boolean isMainSession,
        LocalDateTime lastAccessedAt,
        LocalDateTime migratedAt
) {
}

