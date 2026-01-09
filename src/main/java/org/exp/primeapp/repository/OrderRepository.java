package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Order;
import org.exp.primeapp.models.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusAndCreatedAtAfter(OrderStatus status, LocalDateTime since);

    List<Order> findByStatusAndPaidAtAfter(OrderStatus status, LocalDateTime since);

    List<Order> findByStatusAndConfirmedAtAfter(OrderStatus status, LocalDateTime since);

    List<Order> findByStatusAndDeliveringAtAfter(OrderStatus status, LocalDateTime since);

    List<Order> findByOrderedByUserIdAndStatusIn(Long userId, Collection<OrderStatus> statuses);

    List<Order> findByOrderedByUserIdAndStatusInAndCreatedAtAfter(
            Long userId,
            Collection<OrderStatus> statuses,
            LocalDateTime date);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countTotalOrdersByStatus();

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.createdAt >= :fromDate GROUP BY o.status")
    List<Object[]> countOrdersByStatusSince(@Param("fromDate") LocalDateTime fromDate);
}