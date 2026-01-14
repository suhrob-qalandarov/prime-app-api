package org.exp.primeapp.models.dto.response.admin;

import lombok.Builder;
import org.exp.primeapp.models.entities.InventoryTransaction;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record IncomeStatisticsResponse(
        Long totalCount,
        Integer totalStockQuantity,
        BigDecimal totalIncomeAmount,
        BigDecimal totalUnitPrice,
        BigDecimal averageUnitPrice,
        String filterType,
        String periodStart,
        String periodEnd,
        List<InventoryTransaction> incomes
) {}
