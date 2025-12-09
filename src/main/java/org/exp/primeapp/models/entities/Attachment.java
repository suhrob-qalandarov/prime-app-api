package org.exp.primeapp.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.exp.primeapp.models.base.BaseEntity;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "attachments", indexes = {
        @Index(name = "idx_attachment_url", columnList = "url"),
        @Index(name = "idx_attachment_product_id", columnList = "product_id")
})
public class Attachment extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true, length = 500)
    private String url;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String filePath;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String filename;

    @Column(length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 50)
    private String fileExtension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}
