package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;
import org.exp.primeapp.models.enums.AccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_users")
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false, unique = true)
    private Long telegramId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String firstName;

    private String lastName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String tgUsername;

    private String phone;

    private Integer messageId;

    private Integer verifyCode;

    private LocalDateTime verifyCodeExpiration;

    @Column(name = "ip_address")
    private String ip;

    @Column(name = "browser_info")
    private String browserInfo;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.INACTIVE;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles;
    }

    @Override
    public String getUsername() {
        return this.phone;
    }

    @Override
    public String getPassword() {
        return "[PROTECTED]";
    }

    @Override
    public boolean isEnabled() {
        return this.status == AccountStatus.ACTIVE;
    }
}
