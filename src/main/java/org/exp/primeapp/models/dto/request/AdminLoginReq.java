package org.exp.primeapp.models.dto.request;

public record AdminLoginReq (
        String phoneNumber,
        Integer verifyCode
) {}
