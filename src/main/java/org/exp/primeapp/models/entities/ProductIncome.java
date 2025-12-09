package org.exp.primeapp.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.Inventory;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ProductIncome extends Inventory {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private User userAdmin;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isCalculated = false; // sellingPrice va oneStockPrice/totalIncomeStockPrice bo'lsa true

    @Column(precision = 19, scale = 2)
    private BigDecimal sellingPrice; // Sotiladigan narx
}
