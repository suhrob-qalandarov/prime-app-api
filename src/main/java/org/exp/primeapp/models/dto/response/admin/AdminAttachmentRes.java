package org.exp.primeapp.models.dto.response.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminAttachmentRes(
        String filename,

        String url,

        Boolean isMain,

        Boolean isActive,

        Integer orderNumber,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm")
        LocalDateTime dateTime
) {}
