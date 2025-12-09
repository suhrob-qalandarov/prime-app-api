package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.EntityType;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activity_entity_type", columnList = "entity_type"),
        @Index(name = "idx_activity_entity_id", columnList = "entity_id"),
        @Index(name = "idx_activity_timestamp", columnList = "timestamp"),
        @Index(name = "idx_activity_entity_type_id", columnList = "entity_type, entity_id")
})
public class Activity extends BaseEntity {

    @NotNull
    @Column(name = "entity_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @NotNull
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, etc.

    @Column(columnDefinition = "TEXT")
    private String description; // Activity description

    @Column(length = 255)
    private String performedBy; // User who performed the action

    @Column(columnDefinition = "TEXT")
    private String details; // Additional details as JSON string or text
}

