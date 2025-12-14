package org.exp.primeapp.models.dto.request;

import lombok.Builder;

@Builder
public record CreateSessionReq(
        String ip
) {
}
