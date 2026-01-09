package org.exp.primeapp.models.dto.responce.admin;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminOrderDashRes(
        List<AdminOrderRes> pendingPaymentOrderList,
        List<AdminOrderRes> paidOrderList,
        List<AdminOrderRes> confirmedOrderList,
        List<AdminOrderRes> deliveringOrderList
) {}
