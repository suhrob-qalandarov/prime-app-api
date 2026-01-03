package org.exp.primeapp.models.dto.responce.order;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

import java.time.LocalDateTime;

@Builder
public record UserOrderRes(
                Long id,
                String status,
                String deliveryType,
                LocalDateTime createdAt,
                LocalDateTime deliveredAt,
                List<UserOrderItemRes> items,
                BigDecimal totalSum,
                BigDecimal totalDiscountSum) {
}
