package org.exp.primeapp.models.dto.responce.order;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UserOrderItemRes(
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
        BigDecimal totalSum
) {}
