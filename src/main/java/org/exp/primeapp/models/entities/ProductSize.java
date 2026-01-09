package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.Size;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.exp.primeapp.models.enums.SizeStatus;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_sizes", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id", "size" }))
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ProductSize extends BaseEntity {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Size size;

    @Column(nullable = false)
    private Integer quantity;

    @Builder.Default
    @Column(nullable = false)
    private Integer warningQuantity = 0;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SizeStatus status = SizeStatus.ON_SALE;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalStock = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder.Default
    @Version
    private Integer version = 0;

    @PrePersist
    @PreUpdate
    private void calculateDerivedFields() {
        if (unitPrice != null && quantity != null) {
            this.costPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        if (quantity != null) {
            this.totalStock = quantity;
        }
    }
}
