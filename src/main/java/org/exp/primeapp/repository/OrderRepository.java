package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Order;
import org.exp.primeapp.models.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrderedByUserTelegramId(Long userTelegramId);

    List<Order> findByOrderedByUserIdAndStatus(Long userId, OrderStatus status);

    List<Order> findByOrderedByUser_TelegramIdAndStatus(Long userTelegramId, OrderStatus status);

    List<Order> findByOrderedByUserPhoneAndStatus(String userPhone, OrderStatus status);

    default List<Order> findPendingOrdersByTelegramId(Long userTelegramId) {
        return findByOrderedByUser_TelegramIdAndStatus(userTelegramId, OrderStatus.PENDING);
    }

    default List<Order> findAcceptedOrdersByTelegramId(Long userTelegramId) {
        return findByOrderedByUser_TelegramIdAndStatus(userTelegramId, OrderStatus.CONFIRMED);
    }

    default List<Order> findShippedOrdersByTelegramId(Long userTelegramId) {
        return findByOrderedByUser_TelegramIdAndStatus(userTelegramId, OrderStatus.SHIPPED);
    }
}