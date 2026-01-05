package org.exp.primeapp.controller.admin.order;

import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.admin.AdminOrderDashRes;
import org.exp.primeapp.models.enums.OrderStatus;
import org.exp.primeapp.service.face.admin.order.AdminOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequestMapping(API + V1 + ADMIN + ORDER)
@RequiredArgsConstructor
public class AdminOrderController {
    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<AdminOrderDashRes> getOrderDashboard() {
        AdminOrderDashRes dashboard = adminOrderService.getOrderDashboard();
        return ResponseEntity.ok(dashboard);
    }

    @PutMapping("/{id}/{status}")
    public ResponseEntity<Void> updateOrder(@PathVariable Long id, @PathVariable OrderStatus status) {
        adminOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{id}")
    public void deleteOrder() {

    }
}
