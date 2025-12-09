package org.exp.primeapp.models.dto.responce.global;

import lombok.Builder;

@Builder
public record AttachmentRes(
        Long id,
        String url,
        String filename,
        String originalFilename,
        String contentType,
        Long fileSize,
        String fileExtension
) {
}
