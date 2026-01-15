package org.exp.primeapp.service.impl.global.attachment;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.global.AttachmentRes;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${attachment.max.file.size.mb}")
    private Long maxFileSizeMB;

    @Value("${app.attachment.folder.path:uploads}")
    private String attachmentFolderPath;

    @Value("${app.attachment.base.url:http://localhost:8080}")
    private String attachmentBaseUrl;

    @Override
    public void get(String filenameWithExt, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            log.debug("Fetching attachment: {}", filenameWithExt);

            // 1️⃣ First: Try to read directly from disk (NO DB QUERY!)
            byte[] fileContent = tryReadFromDiskByFilename(filenameWithExt);

            if (fileContent != null) {
                log.debug("✅ File loaded from disk without DB query: {}", filenameWithExt);
                sendFileResponse(response, fileContent, filenameWithExt);
                return;
            }

            // 2️⃣ Fallback: Query DB by filename (native SQL - only base64 field, indexed column!)
            log.info("📦 Disk file not found, attempting database fallback for filename: {}", filenameWithExt);

            String base64Data = attachmentRepository.findBase64DataByFilename(filenameWithExt);

            if (base64Data != null && !base64Data.isEmpty()) {
                byte[] decodedData = Base64.getDecoder().decode(base64Data);
                log.info("✅ File loaded from database (base64 fallback): {} ({} bytes)",
                        filenameWithExt, decodedData.length);
                sendFileResponse(response, decodedData, filenameWithExt);
                return;
            }

            // 3️⃣ File not found anywhere
            log.error("❌ File not found on disk or database: {}", filenameWithExt);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        } catch (Exception e) {
            log.error("Failed to fetch file {}: {}", filenameWithExt, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Try to read file directly from disk using filename
     * NO DATABASE QUERY - super fast!
     */
    private byte[] tryReadFromDiskByFilename(String filename) {
        try {
            Path filePath = Paths.get(attachmentFolderPath, filename);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
        } catch (IOException e) {
            log.warn("⚠️ Error reading file from disk: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Send file response with proper headers
     */
    private void sendFileResponse(HttpServletResponse response, byte[] fileContent, String filename)
            throws IOException {
        String contentType = determineContentType(filename);

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        response.setContentLength(fileContent.length);
        response.setHeader("Cache-Control", "public, max-age=2592000"); // 30 days cache
        response.getOutputStream().write(fileContent);
        response.getOutputStream().flush();
    }

    /**
     * Determine content type from filename extension
     */
    private String determineContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }

        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerFilename.endsWith(".svg")) {
            return "image/svg+xml";
        }

        return "application/octet-stream";
    }

    @Override
    public Attachment getAttachment(String attachmentId) {
        validateAttachmentId(attachmentId);
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Active attachment not found with ID: " + attachmentId));
    }

    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        long maxFileSizeBytes = maxFileSizeMB * 1024 * 1024;
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File size exceeds " + maxFileSizeMB + "MB limit");
        }
    }

    @Override
    public void validateAttachmentId(String attachmentId) {
        if (attachmentId == null || attachmentId.isBlank()) {
            throw new IllegalArgumentException("Invalid attachment ID: " + attachmentId);
        }
    }

    @Override
    public List<AttachmentRes> convertToAttachmentResList(List<Attachment> attachments) {
        return attachments.stream()
                .map(this::convertToAttachmentRes)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> convertToAttachmentUrls(List<Attachment> attachments) {
        return attachments.stream()
                .map(Attachment::getFilename)
                .toList();
    }

    @Override
    public AttachmentRes convertToAttachmentRes(Attachment attachment) {
        return AttachmentRes.builder()
                .id(attachment.getUuid()) // Use UUID as ID
                .url(generateUrl(attachment.getFilename()))
                .filename(attachment.getFilename())
                .originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .fileExtension(attachment.getFileExtension())
                .build();
    }

    @Override
    public String generateUrl(String filename) {
        // Construct full URL with filename (uuid.ext format)
        // Example: http://localhost:8080/api/v1/attachment/uuid-123.jpg
        // This allows direct disk lookup without DB query!
        return attachmentBaseUrl + API + V1 + ATTACHMENT + "/" + filename; // Using filename (uuid.ext) instead of just UUID
    }
}