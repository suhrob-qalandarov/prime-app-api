package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.CategoryStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long orderNumber;

    @Builder.Default
    private String spotlightName = null;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CategoryStatus status = CategoryStatus.CREATED;

    private LocalDateTime lastActivatedAt;

    private LocalDateTime lastDeactivatedAt;
}
