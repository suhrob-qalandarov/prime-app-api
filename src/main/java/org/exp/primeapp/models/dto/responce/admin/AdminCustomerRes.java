package org.exp.primeapp.models.dto.responce.admin;

import lombok.Builder;

@Builder
public record AdminCustomerRes(
        Long id,
        String fullName,
        String phoneNumber,
        Boolean isNew
) {}
