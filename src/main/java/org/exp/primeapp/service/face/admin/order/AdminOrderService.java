package org.exp.primeapp.service.face.admin.order;

import org.exp.primeapp.models.dto.responce.admin.AdminOrderDashRes;
import org.exp.primeapp.models.enums.OrderStatus;

public interface AdminOrderService {
    AdminOrderDashRes getOrderDashboard();

    void updateOrderStatus(Long orderId, OrderStatus status);
}
