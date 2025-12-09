package org.exp.primeapp.models.base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class BaseEntity extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recent_activities", columnDefinition = "jsonb")
    private List<Activity> recentActivities = new ArrayList<>();
}
