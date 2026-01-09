package org.exp.primeapp.models.dto.responce.admin;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminUserRes(
        Long id,
        Long telegramId,
        String firstName,
        String lastName,
        String username,
        String phone,
        Boolean active,
        Boolean isAdmin,
        Boolean isVisitor,
        Boolean isSuperAdmin,
        LocalDateTime createdAt
){}
