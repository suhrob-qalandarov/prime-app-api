package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.SettingCategory;
import org.exp.primeapp.models.enums.SettingType;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settings", indexes = {
        @Index(name = "idx_setting_key", columnList = "key"),
        @Index(name = "idx_setting_category", columnList = "category")
})
public class Setting extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String key;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SettingType type;

    @NotNull
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SettingCategory category = SettingCategory.OTHER;

    @Column(length = 255)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String defaultValue;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isEditable = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVisible = true;

    @Column(length = 500)
    private String validationRule;
}
