package org.exp.primeapp.models.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;

@Builder
public record ProductReq (

        @NotBlank(message = "Mahsulot nomi bo‘sh bo‘lmasligi kerak")
        @Size(max = 512, message = "Mahsulot nomi 512 belgidan oshmasin")
        String name,

        @NotBlank(message = "Brend nomi bo‘sh bo‘lmasligi kerak")
        @Size(max = 512, message = "Brend nomi 512 belgidan oshmasin")
        String brand,

        @NotBlank(message = "Rang nomi bo‘sh bo‘lmasligi kerak")
        @Size(max = 100, message = "Rang nomi 100 belgidan oshmasin")
        String colorName,

        @NotBlank(message = "Rang hex kodi bo‘sh bo‘lmasligi kerak")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Rang hex kodi noto'g'ri formatda (masalan: #FF5733)")
        String colorHex,

        @NotNull(message = "Narx kiritilishi kerak")
        @DecimalMin(value = "0.0", message = "Narx manfiy bo‘lishi mumkin emas")
        BigDecimal price,

        @Size(max = 2048, message = "Tavsif 2048 belgidan oshmasin")
        String description,

        @NotNull(message = "Kategoriya ID kiritilishi kerak")
        @Positive(message = "Kategoriya ID musbat bo‘lishi kerak")
        Long categoryId,

        @NotNull(message = "Rasm yoki fayl URL lari bo‘sh bo‘lmasligi kerak")
        @Size(max = 10, message = "Eng ko‘pi bilan 10 ta fayl yuklash mumkin")
        Set<String> attachmentUrls
) {}
