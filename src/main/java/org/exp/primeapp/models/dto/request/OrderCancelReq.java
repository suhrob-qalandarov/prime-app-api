package org.exp.primeapp.models.dto.request;

import lombok.Builder;
import org.exp.primeapp.models.enums.CancelReason;

@Builder
public record OrderCancelReq(
        CancelReason reason,
        String comment
) {}
