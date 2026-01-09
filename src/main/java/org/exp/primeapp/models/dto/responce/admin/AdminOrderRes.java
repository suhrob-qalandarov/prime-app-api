package org.exp.primeapp.models.dto.responce.admin;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record AdminOrderRes(
        Long id,
        String status,
        AdminCustomerRes customer,
        String customerComment,
        String deliveryType,
        BigDecimal totalPrice,
        String dateTime,
        List<AdminOrderItemRes> items
) {}
