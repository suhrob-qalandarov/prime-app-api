package org.exp.primeapp.models.dto.request;

import lombok.Builder;

@Builder
public record CustomerReq(
        String fullName,
        String phoneNumber,
        String comment
) {}
