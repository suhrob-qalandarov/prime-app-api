package org.exp.primeapp.models.dto.responce.user;

import lombok.Builder;
import org.exp.primeapp.models.dto.responce.order.UserProfileOrdersRes;

@Builder
public record UserRes(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String username,
        UserProfileOrdersRes orders,
        Boolean isAdmin,
        Boolean isVisitor,
        Boolean isSuperAdmin
){}
