package org.exp.primeapp.models.dto.responce.user;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ProductRes(
        Long id,
        String name,
        String brand,
        String colorName,
        String colorHex,
        String tag,
        String category,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer discount,
        List<String> images,
        List<ProductSizeRes> sizes
) {}
