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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${attachment.max.file.size.mb}")
    private Long maxFileSizeMB;

    @Value("${app.attachment.folder.path:uploads}")
    private String attachmentFolderPath;

    @Override
    public void get(String attachmentUrl, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Attachment attachment = getAttachmentWithUrl(attachmentUrl);
            if (attachment == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Simple check: if file exists?
            // Assuming attachmentUrl is "filename.ext" or "UUID.ext"
            // If DB entry exists, try to get file.

            byte[] fileContent = getFileContent(attachment.getFilename()); // Assuming filename is stored
            if (fileContent == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String filename = attachment.getOriginalFilename() != null
                    ? attachment.getOriginalFilename()
                    : attachment.getFilename();

            response.setContentType(attachment.getContentType() != null ? attachment.getContentType() : "image/jpeg");
            response.setHeader("Content-Disposition",
                    "inline; filename=\"" + (filename != null ? filename : "attachment") + "\"");
            response.setContentLength(fileContent.length);
            response.getOutputStream().write(fileContent);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("Failed to fetch file for attachment URL {}: {}", attachmentUrl, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new IOException("Unable to retrieve file", e);
        }
    }

    private byte[] getFileContent(String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        Path filePath = Paths.get(attachmentFolderPath, filename);
        if (!Files.exists(filePath)) {
            return null;
        }
        return Files.readAllBytes(filePath);
    }

    @Override
    public Attachment getAttachment(Long attachmentId) {
        validateAttachmentId(attachmentId);
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Active attachment not found with ID: " + attachmentId));
    }

    @Override
    public Attachment getAttachmentWithUrl(String attachmentUrl) {
        return attachmentRepository.findByUrl(attachmentUrl);
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
    public void validateAttachmentId(Long attachmentId) {
        if (attachmentId == null || attachmentId <= 0) {
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
                .map(Attachment::getUrl).toList();
    }

    @Override
    public AttachmentRes convertToAttachmentRes(Attachment attachment) {
        return AttachmentRes.builder()
                .id(attachment.getId())
                .url(attachment.getUrl())
                .filename(attachment.getFilename())
                .originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .fileExtension(attachment.getFileExtension())
                .build();
    }
}