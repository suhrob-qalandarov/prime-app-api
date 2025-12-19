package org.exp.primeapp.models.dto.responce.admin;

import lombok.Builder;
import org.exp.primeapp.models.dto.responce.user.SessionRes;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminUserRes(
        Long id,
        Long telegramId,
        String firstName,
        String lastName,
        String username,
        String phone,
        List<SessionRes> sessions,
        Boolean active,
        Boolean isAdmin,
        Boolean isVisitor,
        Boolean isSuperAdmin,
        LocalDateTime createdAt
) {
}
