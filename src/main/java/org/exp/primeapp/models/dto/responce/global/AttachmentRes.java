package org.exp.primeapp.models.dto.responce.global;

import lombok.Builder;

@Builder
public record AttachmentRes(
        String id,
        String url,
        String filename,
        String originalFilename,
        String contentType,
        Long fileSize,
        String fileExtension
) {}
