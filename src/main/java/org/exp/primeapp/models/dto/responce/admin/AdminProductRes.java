package org.exp.primeapp.models.dto.responce.admin;

import lombok.Builder;
import org.exp.primeapp.models.dto.responce.user.ProductSizeRes;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record AdminProductRes(
        Long id,
        String name,
        String brand,
        String colorName,
        String colorHex,
        String description,
        String categoryName,
        BigDecimal price,
        String tag,
        Boolean active,
        Integer discount,
        String createdAt,
        List<String> picturesUrls,
        List<ProductSizeRes> sizes
){}
