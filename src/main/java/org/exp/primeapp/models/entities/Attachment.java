package org.exp.primeapp.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
        @Index(name = "idx_attachment_url", columnList = "url")
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
}
