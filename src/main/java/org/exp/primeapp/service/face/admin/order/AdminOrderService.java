package org.exp.primeapp.service.face.admin.order;

import org.exp.primeapp.models.dto.responce.admin.AdminOrderDashRes;
import org.springframework.stereotype.Service;

@Service
public interface AdminOrderService {
    AdminOrderDashRes getOrderDashboard();
}
