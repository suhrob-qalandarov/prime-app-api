package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_customers")
public class Customer extends BaseEntity {

    @Column(columnDefinition = "TEXT", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private Integer orderAmount;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isNew = true;

    @ManyToOne
    @JoinColumn(name = "user_profile_id", nullable = false)
    private User profile;
}
