package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.TransactionReason;
import org.exp.primeapp.models.enums.TransactionType;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction extends BaseEntity {

    @Column(nullable = false, name = "product_name")
    private String productName;

    @Column(nullable = false, name = "product_description")
    private String productDescription;

    @Column(nullable = false, name = "product_main_image_url")
    private String productMainImageUrl;

    @Column(name = "product_tag_name")
    private String productTag;

    @Column(name = "product_status_name")
    private String productStatus;

    @Column(name = "product_color_name")
    private String productColor;

    @Column(name = "product_color_hex")
    private String productColorHex;

    @Column(nullable = false, name = "product_category_name")
    private String productCategoryName;

    @Column(name = "product_size_name")
    private String productSize;

    @Column(nullable = false, name = "stock_quantity")
    private Integer stockQuantity;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2, name = "unit_price")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, name = "discount_percent")
    @Min(0)
    @Max(100)
    private Integer discountPercent = 0;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2, name = "total_price")
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionReason reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Customer client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_admin_id")
    private User performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_performed_admin_id")
    private User returnPerformedBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    @LastModifiedDate
    @Column(name = "returned_at")
    protected LocalDateTime returnedAt;
}
