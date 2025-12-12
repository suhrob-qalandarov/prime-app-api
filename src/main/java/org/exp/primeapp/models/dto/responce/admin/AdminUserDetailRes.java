package org.exp.primeapp.models.dto.responce.admin;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminUserDetailRes(
        Long id,
        Long telegramId,
        String firstName,
        String lastName,
        String tgUsername,
        String phone,
        List<String> roles,
        Boolean active,
        Integer messageId,
        Integer verifyCode
) {
}

