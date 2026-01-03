package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Order;
import org.exp.primeapp.models.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrderedByUserIdAndStatusIn(Long userId, Collection<OrderStatus> statuses);

    List<Order> findByOrderedByUserIdAndStatusInAndCreatedAtAfter(
            Long userId,
            Collection<OrderStatus> statuses,
            LocalDateTime date
    );
}