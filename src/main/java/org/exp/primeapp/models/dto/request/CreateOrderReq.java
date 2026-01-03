package org.exp.primeapp.models.dto.request;

import lombok.Builder;
import org.exp.primeapp.models.enums.OrderDeliveryType;

import java.util.List;

@Builder
public record CreateOrderReq(
        CustomerReq customer,
        OrderDeliveryType delivery,
        List<CreateOrderItemReq> items
) {}
