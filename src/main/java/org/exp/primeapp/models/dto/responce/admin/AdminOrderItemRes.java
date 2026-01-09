package org.exp.primeapp.models.dto.responce.admin;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AdminOrderItemRes(
        String name,
        String brand,
        String colorName,
        String colorHex,
        String mainImageUrl,
        String size,
        BigDecimal price,
        Integer discount,
        Integer discountPrice,
        Integer amount,
        BigDecimal totalSum,
        Integer stock
) {}
