package org.exp.primeapp.models.dto.response.admin;

import lombok.Builder;
import org.exp.primeapp.models.entities.ProductIncome;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record IncomeStatisticsResponse(
        // Umumiy statistika
        Long totalCount,                    // Jami income soni
        Integer totalStockQuantity,         // Jami stock miqdori
        BigDecimal totalIncomeAmount,       // Jami income summasi (totalPrice)
        BigDecimal totalUnitPrice,          // Jami unit price
        BigDecimal averageUnitPrice,        // O'rtacha unit price
        
        // Income ro'yxati
        List<ProductIncome> incomes,
        
        // Filter ma'lumotlari
        String filterType,                  // TODAY, WEEKLY, MONTHLY
        String periodStart,                 // Period boshlanish sanasi
        String periodEnd                    // Period tugash sanasi
) {}

