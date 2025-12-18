package org.exp.primeapp.models.dto.responce.user;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductPageRes(
        Long id,
        String name,
        String brand,
        String colorHex,
        String tag,
        BigDecimal price,
        BigDecimal discountPrice,
        Integer discount,
        String mainImage
) {}

