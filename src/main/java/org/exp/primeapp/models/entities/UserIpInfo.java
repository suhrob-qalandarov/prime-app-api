package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_ip_infos", indexes = {
        @Index(name = "idx_user_ip_info_user_id", columnList = "user_id"),
        @Index(name = "idx_user_ip_info_is_register", columnList = "is_register_info"),
        @Index(name = "idx_user_ip_info_user_register", columnList = "user_id, is_register_info")
})
public class UserIpInfo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 45, nullable = false)
    private String ip;

    @Column(length = 500)
    private String browserInfo;

    @Column(nullable = false)
    private LocalDateTime accessedAt;

    @Column(name = "is_register_info", nullable = false)
    @Builder.Default
    private Boolean isRegisterInfo = false; // true bo'lsa register IP, false bo'lsa login IP
}

