package org.exp.primeapp.models.entities;

import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.ProductTag;
import org.exp.primeapp.models.enums.ProductStatus;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String brand;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Integer discount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer salesCount = 0;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductTag tag = ProductTag.NEW;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.PENDING_INCOME;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductSize> sizes = new HashSet<>();

    private List<String> recentActivities = new ArrayList<>();
}

/*
public void addSize(ProductSize productSize) {
    ProductSize existing = sizes.stream()
            .filter(ps -> ps.getSize().equals(productSize.getSize()))
            .findFirst()
            .orElse(null);

    if (existing == null) {
        productSize.setProduct(this);
        sizes.add(productSize);
    } else {
        int newAmount = existing.getAmount() + productSize.getAmount();
        existing.setAmount(newAmount);
    }
}
*/