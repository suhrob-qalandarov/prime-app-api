package org.exp.primeapp.models.base;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.entities.Product;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class Inventory extends BaseEntity {

    @Column(nullable = false, name = "stock_quantity")
    private Integer stockQuantity;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2, name = "unit_price")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2, name = "total_price")
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
