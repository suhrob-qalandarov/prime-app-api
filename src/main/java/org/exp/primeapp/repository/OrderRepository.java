package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Order;
import org.exp.primeapp.models.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrderedByUserIdAndStatusIn(Long userId, List<OrderStatus> statuses);
}