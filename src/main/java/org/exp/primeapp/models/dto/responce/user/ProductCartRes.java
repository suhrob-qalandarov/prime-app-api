package org.exp.primeapp.models.dto.responce.user;

import java.math.BigDecimal;

public record ProductCartRes(
        Long id,
        String name,
        String brand,
        String colorName,
        String colorHex,
        String chosenSize,
        BigDecimal price,
        BigDecimal discountPrice,
        String mainImage,
        Boolean hasEnough,
        Integer available,
        Integer quantity,
        BigDecimal totalPrice
) {
}

