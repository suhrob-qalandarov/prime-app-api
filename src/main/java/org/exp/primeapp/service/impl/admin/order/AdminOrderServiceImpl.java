package org.exp.primeapp.service.impl.admin.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.admin.AdminCustomerRes;
import org.exp.primeapp.models.dto.responce.admin.AdminOrderDashRes;
import org.exp.primeapp.models.dto.responce.admin.AdminOrderItemRes;
import org.exp.primeapp.models.dto.responce.admin.AdminOrderRes;
import org.exp.primeapp.models.entities.Order;
import org.exp.primeapp.models.entities.OrderItem;
import org.exp.primeapp.models.enums.OrderStatus;
import org.exp.primeapp.repository.OrderRepository;
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
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        private static final int DAYS_FILTER = 10;

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
                // SHIPPED - filter by deliveringAt
                List<AdminOrderRes> deliveringOrders = getOrdersByStatusWithDeliveringAt(since);

                return AdminOrderDashRes.builder()
                                .pendingPaymentOrderList(pendingPaymentOrders)
                                .paidOrderList(paidOrders)
                                .confirmedOrderList(confirmedOrders)
                                .deliveringOrderList(deliveringOrders)
                                .build();
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
                List<Order> orders = orderRepository.findByStatusAndDeliveringAtAfter(OrderStatus.SHIPPED, since);
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
