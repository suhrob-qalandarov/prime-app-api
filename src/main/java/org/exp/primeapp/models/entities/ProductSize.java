package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.Size;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "size"}))
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ProductSize extends BaseEntity {

    @Column(nullable = false)
    private Integer amount;

    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Size size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Builder.Default
    @Version
    private Integer version = 0;

    @PostLoad
    @PrePersist
    @PreUpdate
    private void generateSku() {
        if (this.product != null && this.size != null) {
            String brand = this.product.getBrand().trim().toUpperCase().replace(" ", "");
            String name = this.product.getName().trim().toUpperCase().replace(" ", "");
            String sizeCode = this.size.name();

            this.sku = brand + "-" + name + "-" + sizeCode;
        }
    }
}
