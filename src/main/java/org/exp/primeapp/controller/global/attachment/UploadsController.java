package org.exp.primeapp.controller.global.attachment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UploadsController {

    @Value("${app.attachment.folder.path:uploads}")
    private String attachmentFolderPath;

    /**
     * Attachment olish - /uploads/** path uchun (frontend uchun)
     * Database'ga so'rov yubormasdan, faqat folder'dan file'ni serve qiladi
     */
    @GetMapping("/uploads/**")
    public ResponseEntity<?> getAttachmentByPath(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // Path'dan filename'ni extract qilish
            String path = request.getRequestURI();
            String filename = path.substring(path.lastIndexOf("/uploads/") + "/uploads/".length());

            // File path'ni yaratish
            Path filePath = Paths.get(attachmentFolderPath, filename);

            // File mavjudligini tekshirish
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return ResponseEntity.notFound().build();
            }

            // Content type'ni aniqlash
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "image/jpeg"; // Default
            }

            // File'ni o'qib, response'ga yozish
            byte[] fileContent = Files.readAllBytes(filePath);

            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
            response.setContentLength(fileContent.length);
            response.getOutputStream().write(fileContent);
            response.getOutputStream().flush();

            log.info("Successfully served attachment by path: {}", path);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to fetch attachment by path: {}", request.getRequestURI(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new RuntimeException("Failed to fetch attachment: " + e.getMessage(), e);
        }
    }
}
