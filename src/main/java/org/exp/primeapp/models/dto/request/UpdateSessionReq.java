package org.exp.primeapp.models.dto.request;

import lombok.Builder;

import java.util.LinkedHashSet;

@Builder
public record UpdateSessionReq(
        String ip,
        LinkedHashSet<String> browserInfos,
        Boolean isActive,
        Boolean isAuthenticated
) {
}
