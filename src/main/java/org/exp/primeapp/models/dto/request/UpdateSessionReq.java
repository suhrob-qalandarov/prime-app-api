package org.exp.primeapp.models.dto.request;

import lombok.Builder;

@Builder
public record UpdateSessionReq(
        String ip,
        String browserInfo,
        Boolean isActive,
        Boolean isAuthenticated
) {
}
