package org.exp.primeapp.models.base;

import jakarta.persistence.Column;
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

    @Column(nullable = false)
    private Integer amount;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal totalCostPrice = BigDecimal.ZERO;

    @ManyToOne
    private Product product;
}
