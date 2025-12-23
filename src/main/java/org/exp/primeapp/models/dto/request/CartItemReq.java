package org.exp.primeapp.models.dto.request;

public record CartItemReq(
        Long productId,
        String productSize,  // Size enum label yoki name (masalan "L" yoki "SIZE_41")
        Integer productQuantity
) {
}

