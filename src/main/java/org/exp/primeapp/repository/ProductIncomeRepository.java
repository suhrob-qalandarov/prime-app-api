package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.ProductIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductIncomeRepository extends JpaRepository<ProductIncome, Long>, ProductIncomeRepositoryCustom {
    
    // Date range bo'yicha income larni topish
    List<ProductIncome> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    
    // Date range bo'yicha income larni topish (createdAt bo'yicha)
    @Query("SELECT pi FROM ProductIncome pi WHERE pi.createdAt >= :start AND pi.createdAt <= :end ORDER BY pi.createdAt DESC")
    List<ProductIncome> findIncomesByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}