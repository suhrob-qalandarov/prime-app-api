package org.exp.primeapp.models.dto.request;

import lombok.Builder;

@Builder
public record CreateOrderItemReq(
        Long productId,
        Long sizeId,
        Integer amount
) {}
