package org.exp.primeapp.models.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.exp.primeapp.models.enums.Size;

import java.math.BigDecimal;

@Builder
public record IncomeRequest(
        @NotNull(message = "Product ID kiritilishi kerak")
        @Positive(message = "Product ID musbat bo'lishi kerak")
        Long productId,

        @NotNull(message = "Size kiritilishi kerak")
        Size size,

        @NotNull(message = "Income stock miqdori kiritilishi kerak")
        @Positive(message = "Income stock miqdori musbat bo'lishi kerak")
        Integer incomeStock,

        // Bitta mahsulot narxi (optional)
        BigDecimal oneStockPrice,

        // Umumiy income stock narxi (optional)
        BigDecimal totalIncomeStockPrice,

        // Sotiladigan narx (optional)
        BigDecimal sellingPrice,

        // Product price ga set qilish uchun flag
        Boolean canSetPriceToProduct
) {
    public IncomeRequest {
        if (canSetPriceToProduct == null) {
            canSetPriceToProduct = false;
        }
    }
}

