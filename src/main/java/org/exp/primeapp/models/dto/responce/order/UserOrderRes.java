package org.exp.primeapp.models.dto.responce.order;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record UserOrderRes(
                Long number,
                String status,
                String deliveryType,
                String createdAt,
                String deliveredAt,
                List<UserOrderItemRes> items,
                BigDecimal totalSum,
                BigDecimal totalDiscountSum) {
}
