package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.Auditable;
import org.exp.primeapp.utils.converter.LinkedHashSetStringConverter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sessions", indexes = {
    @Index(name = "idx_session_session_id", columnList = "session_id", unique = true),
    @Index(name = "idx_session_user_id", columnList = "user_id"),
    @Index(name = "idx_session_ip", columnList = "ip"),
    @Index(name = "idx_session_is_deleted", columnList = "is_deleted")
})
public class Session extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, unique = true, length = 100)
    private String sessionId; // Unique session identifier (UUID) - database yaratadi

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // User ro'yxatdan o'tsa biriktiriladi (nullable)

    @Column(nullable = false, length = 45)
    private String ip; // Client IP address

    @Column(columnDefinition = "TEXT")
    @Convert(converter = LinkedHashSetStringConverter.class)
    private LinkedHashSet<String> browserInfos; // User-Agent list (unique, ordered)

    @Column(length = 1000)
    private String accessToken; // JWT authentication token

    @Column(length = 500)
    private String attachmentToken; // Attachment access token

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // Session active yoki yo'q

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false; // Session o'chirilgan yoki yo'q

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAuthenticated = false; // Session authenticated yoki yo'q

    @Column(nullable = false)
    @Builder.Default
    private Boolean isMainSession = false; // User ning birinchi session i yoki yo'q

    @Column(nullable = false)
    private LocalDateTime lastAccessedAt; // Oxirgi marta foydalanilgan vaqt

    @Column
    private LocalDateTime migratedAt; // User ga migrate qilingan vaqt
}

