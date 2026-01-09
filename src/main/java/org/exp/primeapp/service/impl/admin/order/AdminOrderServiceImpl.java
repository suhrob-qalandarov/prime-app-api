package org.exp.primeapp.service.impl.admin.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.OrderCancelReq;
import org.exp.primeapp.models.dto.responce.admin.AdminCustomerRes;
import org.exp.primeapp.models.dto.responce.admin.AdminOrderDashRes;
import org.exp.primeapp.models.dto.responce.admin.AdminOrderItemRes;
import org.exp.primeapp.models.dto.responce.admin.AdminOrderRes;
import org.exp.primeapp.models.entities.Order;
import org.exp.primeapp.models.entities.OrderItem;
import org.exp.primeapp.models.entities.ProductSize;
import org.exp.primeapp.models.enums.OrderStatus;
import org.exp.primeapp.repository.OrderRepository;
import org.exp.primeapp.repository.ProductSizeRepository;
import org.exp.primeapp.service.face.admin.order.AdminOrderService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

        private final OrderRepository orderRepository;
        private final ProductSizeRepository productSizeRepository;

        private static final int DAYS_FILTER = 10;
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        @Transactional
        @Override
        public AdminOrderDashRes getOrderDashboard() {
                LocalDateTime since = LocalDateTime.now().minusDays(DAYS_FILTER);

                // PENDING_PAYMENT - filter by createdAt
                List<AdminOrderRes> pendingPaymentOrders = getOrdersByStatusWithCreatedAt(since);
                // PAID - filter by paidAt
                List<AdminOrderRes> paidOrders = getOrdersByStatusWithPaidAt(since);
                // CONFIRMED - filter by confirmedAt
                List<AdminOrderRes> confirmedOrders = getOrdersByStatusWithConfirmedAt(since);
                // DELIVERING - filter by deliveringAt
                List<AdminOrderRes> deliveringOrders = getOrdersByStatusWithDeliveringAt(since);

                return AdminOrderDashRes.builder()
                                .pendingPaymentOrderList(pendingPaymentOrders)
                                .paidOrderList(paidOrders)
                                .confirmedOrderList(confirmedOrders)
                                .deliveringOrderList(deliveringOrders)
                                .build();
        }

        @Transactional
        @Override
        public void updateOrderStatus(Long orderId, OrderStatus nextStatus) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

                OrderStatus currentStatus = order.getStatus();

                // 1. Validate status transition
                if (!isValidTransition(currentStatus, nextStatus)) {
                        throw new RuntimeException(
                                        "Invalid status transition from " + currentStatus + " to " + nextStatus);
                }

                // 2. Handle stock deduction when moving TO PAID status
                if (nextStatus == OrderStatus.PAID && currentStatus == OrderStatus.PENDING_PAYMENT) {
                        deductStockWithLock(order);
                }

                // 3. Check stock availability for other forward transitions (if needed)
                // But if it's already PAID, stock was already deducted.
                if (nextStatus != OrderStatus.CANCELLED && nextStatus != OrderStatus.REFUNDED
                                && nextStatus != OrderStatus.PAID) {
                        checkStockAvailability(order);
                }

                // 4. Handle stock return if cancelling from a status that already had stock
                // deducted
                if (nextStatus == OrderStatus.CANCELLED) {
                        // Only return stock if it was already deducted (i.e., status was PAID or
                        // further)
                        if (currentStatus != OrderStatus.PENDING_PAYMENT) {
                                returnStockToProductSizes(order);
                        }
                        order.setCancelledAt(LocalDateTime.now());
                }

                // 5. Update timestamps based on status
                updateStatusTimestamps(order, nextStatus);

                // 6. Update status
                order.setStatus(nextStatus);
                orderRepository.save(order);
        }

        @Transactional
        @Override
        public void cancelOrder(Long orderId, OrderCancelReq cancelReq) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

                // Cannot cancel already cancelled or refunded orders
                if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
                        throw new RuntimeException("Order is already cancelled or refunded");
                }

                // Return stock if order was PAID or further
                if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                        returnStockToProductSizes(order);
                }

                // Set cancel reason and admin comment
                order.setCancelReason(cancelReq.reason());
                order.setAdminComment(cancelReq.comment());
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(LocalDateTime.now());

                orderRepository.save(order);
        }

        private void deductStockWithLock(Order order) {
                for (OrderItem item : order.getItems()) {
                        ProductSize ps = productSizeRepository
                                        .findByIdWithLock(item.getProductSize().getId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Product size not found: " + item.getProductSize().getId()));

                        if (ps.getQuantity() < item.getQuantity()) {
                                throw new RuntimeException("Yetarli mahsulot mavjud emas: " + item.getName() +
                                                " (Zaxira: " + ps.getQuantity() + ", So'ralgan: " + item.getQuantity()
                                                + ")");
                        }

                        ps.setQuantity(ps.getQuantity() - item.getQuantity());
                        productSizeRepository.save(ps);
                }
        }

        private void checkStockAvailability(Order order) {
                for (OrderItem item : order.getItems()) {
                        // If we are already past PAID, quantity should be fine, but we can do a safety
                        // check
                        if (item.getProductSize().getQuantity() < 0) {
                                throw new RuntimeException("Zaxira bilan bog'liq xatolik: " + item.getName());
                        }
                }
        }

        private boolean isValidTransition(OrderStatus current, OrderStatus next) {
                if (next == OrderStatus.CANCELLED)
                        return true;
                if (current == OrderStatus.CANCELLED || current == OrderStatus.REFUNDED)
                        return false;
                if (next == OrderStatus.REFUNDED)
                        return current != OrderStatus.PENDING_PAYMENT;

                // General rule: status can only move forward
                return next.ordinal() > current.ordinal();
        }

        private void returnStockToProductSizes(Order order) {
                for (OrderItem item : order.getItems()) {
                        ProductSize ps = item.getProductSize();
                        ps.setQuantity(ps.getQuantity() + item.getQuantity());
                        productSizeRepository.save(ps);
                }
        }

        private void updateStatusTimestamps(Order order, OrderStatus status) {
                LocalDateTime now = LocalDateTime.now();
                switch (status) {
                        case PAID -> order.setPaidAt(now);
                        case CONFIRMED -> order.setConfirmedAt(now);
                        case DELIVERING -> order.setDeliveringAt(now);
                        case SHIPPED -> {
                                // If deliveringAt is already set (e.g. via taxi), we don't necessarily need to
                                // overwrite it,
                                // but for SHIPPED (BTS) we might use the same field if no shippedAt exists.
                                if (order.getDeliveringAt() == null) {
                                        order.setDeliveringAt(now);
                                }
                        }
                        case DELIVERED -> order.setDeliveredAt(now);
                        case REFUNDED -> order.setRefundedAt(now);
                        case CANCELLED -> order.setCancelledAt(now);
                        default -> {
                        }
                }
        }

        private List<AdminOrderRes> getOrdersByStatusWithCreatedAt(LocalDateTime since) {
                List<Order> orders = orderRepository.findByStatusAndCreatedAtAfter(OrderStatus.PENDING_PAYMENT, since);
                return orders.stream()
                                .map(order -> convertToAdminOrderRes(order, order.getCreatedAt()))
                                .collect(Collectors.toList());
        }

        private List<AdminOrderRes> getOrdersByStatusWithPaidAt(LocalDateTime since) {
                List<Order> orders = orderRepository.findByStatusAndPaidAtAfter(OrderStatus.PAID, since);
                return orders.stream()
                                .map(order -> convertToAdminOrderRes(order, order.getPaidAt()))
                                .collect(Collectors.toList());
        }

        private List<AdminOrderRes> getOrdersByStatusWithConfirmedAt(LocalDateTime since) {
                List<Order> orders = orderRepository.findByStatusAndConfirmedAtAfter(OrderStatus.CONFIRMED, since);
                return orders.stream()
                                .map(order -> convertToAdminOrderRes(order, order.getConfirmedAt()))
                                .collect(Collectors.toList());
        }

        private List<AdminOrderRes> getOrdersByStatusWithDeliveringAt(LocalDateTime since) {
                List<Order> orders = orderRepository.findByStatusAndDeliveringAtAfter(OrderStatus.DELIVERING, since);
                return orders.stream()
                                .map(order -> convertToAdminOrderRes(order, order.getDeliveringAt()))
                                .collect(Collectors.toList());
        }

        private AdminOrderRes convertToAdminOrderRes(Order order, LocalDateTime statusDateTime) {
                String formattedDateTime = statusDateTime != null
                                ? statusDateTime.format(DATE_FORMATTER)
                                : order.getCreatedAt().format(DATE_FORMATTER);

                return AdminOrderRes.builder()
                                .id(order.getId())
                                .status(order.getStatus().name())
                                .customer(AdminCustomerRes.builder()
                                                .id(order.getCustomer().getId())
                                                .fullName(order.getCustomer().getFullName())
                                                .phoneNumber(order.getCustomer().getPhoneNumber())
                                                .isNew(order.getCustomer().getIsNew())
                                                .build())
                                .customerComment(order.getComment())
                                .deliveryType(order.getShippingType().name())
                                .totalPrice(order.getTotalPrice())
                                .dateTime(formattedDateTime)
                                .items(order.getItems().stream()
                                                .map(this::convertToAdminOrderItemRes)
                                                .collect(Collectors.toList()))
                                .build();
        }

        private AdminOrderItemRes convertToAdminOrderItemRes(OrderItem item) {
                BigDecimal discountedPrice = item.getUnitPrice()
                                .multiply(BigDecimal.valueOf(100 - item.getDiscountPercent()))
                                .divide(BigDecimal.valueOf(100));
                BigDecimal totalSum = discountedPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                return AdminOrderItemRes.builder()
                                .name(item.getName())
                                .brand(item.getBrand())
                                .colorName(item.getColorName())
                                .colorHex(item.getColorHex())
                                .mainImageUrl(item.getImageUrl())
                                .size(item.getSize())
                                .price(item.getUnitPrice())
                                .discount(item.getDiscountPercent())
                                .discountPrice(discountedPrice.intValue())
                                .amount(item.getQuantity())
                                .totalSum(totalSum)
                                .stock(item.getProductSize().getQuantity())
                                .build();
        }
}
