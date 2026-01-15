package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.Auditable;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "attachments", indexes = {
        @Index(name = "idx_attachment_product_id", columnList = "product_id")
})
public class Attachment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "is_main")
    private Boolean isMain;

    @Column(name = "is_active")
    private Boolean isActive;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String filePath;

    @NotBlank
    @Column(nullable = false, length = 300)
    private String filename;

    @Column(length = 300)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 50)
    private String fileExtension;

    @Column(name = "file_timestamp")
    private Long fileTimestamp;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data_base64", columnDefinition = "TEXT")
    private String fileDataBase64;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}
