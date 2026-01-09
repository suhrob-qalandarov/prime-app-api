package org.exp.primeapp.models.entities;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.ProductTag;
import org.exp.primeapp.models.enums.ProductStatus;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
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
    private String brand;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String colorName;

    @Column(nullable = false)
    private String colorHex;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    @Min(0)
    @Max(100)
    private Integer discountPercent = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer salesCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer warningQuantity = 0;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductTag tag = ProductTag.NEW;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.PENDING_INCOME;

    @Column(nullable = false)
    private String categoryName;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductSize> sizes = new HashSet<>();

    private LocalDateTime publishedAt;

    private LocalDateTime lastActivatedAt;

    private LocalDateTime lastOutOfStockAt;

    private LocalDateTime lastDeactivatedAt;

    private LocalDateTime lastArchivedAt;
}