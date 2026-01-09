package org.exp.primeapp.models.dto.responce.admin;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminProductDashboardRes(
        long totalCount,
        long activeCount,
        long inactiveCount,
        long newTagCount,
        long hotTagCount,
        long saleTagCount,
        LocalDateTime responseDate,
        List<AdminProductRes> products
){}
