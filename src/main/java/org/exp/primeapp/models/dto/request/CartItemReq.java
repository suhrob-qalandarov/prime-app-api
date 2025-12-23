package org.exp.primeapp.models.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CartItemReq(
        @JsonProperty("productId")
        Long productId,
        
        @JsonProperty("productSize")
        String productSize,  // Size enum label yoki name (masalan "L" yoki "SIZE_41")
        
        @JsonProperty("productQuantity")
        Integer productQuantity
) {
}

