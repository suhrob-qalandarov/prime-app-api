package org.exp.primeapp.service.impl.admin.attachment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.global.AttachmentRes;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.service.face.admin.attachment.AdminAttachmentService;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAttachmentServiceImpl implements AdminAttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;

    @Value("${app.attachment.folder.path:uploads}")
    private String attachmentFolderPath;

    @Override
    public List<AttachmentRes> getAttachments() {
        List<Attachment> all = attachmentRepository.findAll();
        return attachmentService.convertToAttachmentResList(all);
    }

    @Override
    public List<AttachmentRes> getAttachmentsNoProduct() {
        List<Attachment> noProduct = attachmentRepository.findByProductIsNull();
        return attachmentService.convertToAttachmentResList(noProduct);
    }

    @Override
    public List<AttachmentRes> getAttachmentsLinkedWithProduct() {
        List<Attachment> linkedToProduct = attachmentRepository.findByProductIsNotNull();
        return attachmentService.convertToAttachmentResList(linkedToProduct);
    }

    @Transactional
    @Override
    public Attachment uploadOne(MultipartFile file) {
        attachmentService.validateFile(file);
        String filename = saveFileLocally(file);
        String base64Data = convertToBase64(file);
        return saveAttachment(file, filename, base64Data, false);
    }

    @Override
    public Attachment uploadMainFile(MultipartFile file) {
        attachmentService.validateFile(file);
        String filename = saveFileLocally(file);
        String base64Data = convertToBase64(file);
        return saveAttachment(file, filename, base64Data, true);
    }


    @Transactional
    @Override
    public List<Attachment> uploadMultiple(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Files cannot be null or empty");
        }
        return Arrays.stream(files)
                .map(file -> {
                    attachmentService.validateFile(file);
                    String filename = saveFileLocally(file);
                    String base64Data = convertToBase64(file);
                    return saveAttachment(file, filename, base64Data, false);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public AttachmentRes update(String attachmentId, MultipartFile file) {
        attachmentService.validateFile(file);
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        String oldFilename = attachment.getFilename();
        String newFilename = saveFileLocally(file);
        String base64Data = convertToBase64(file);

        attachment.setFilename(newFilename);
        attachment.setOriginalFilename(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFileExtension(getFileExtension(newFilename));
        attachment.setFileDataBase64(base64Data);

        try {
            Attachment saved = attachmentRepository.save(attachment);
            if (oldFilename != null && !oldFilename.equals(newFilename)) {
                deleteLocalFile(oldFilename);
            }
            return attachmentService.convertToAttachmentRes(saved);
        } catch (Exception e) {
            log.error("Failed to update attachment ID {} in database: {}", attachmentId, e.getMessage());
            deleteLocalFile(newFilename); // Rollback new file upload
            throw new RuntimeException("Unable to update attachment in database", e);
        }
    }

    @Transactional
    @Override
    public void delete(String attachmentId) {
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        String filename = attachment.getFilename();

        try {
            attachmentRepository.delete(attachment);
            deleteLocalFile(filename);
        } catch (Exception e) {
            log.error("Failed to delete attachment ID {}: {}", attachmentId, e.getMessage());
            throw new RuntimeException("Unable to delete attachment in database", e);
        }
    }

    @Transactional
    @Override
    public void deleteFromS3(String attachmentId) {
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        String filename = attachment.getFilename();

        if (filename.startsWith("deleted_")) {
            return;
        }

        try {
            attachment.setFilename("deleted_" + filename);
            attachmentRepository.save(attachment);
        } catch (Exception e) {
            log.error("Failed to soft-delete attachment ID {}: {}", attachmentId, e.getMessage());
            throw new RuntimeException("Unable to soft-delete attachment in database", e);
        }
        // Assuming we keep the file but rename it or something, but the original code
        // deleted it from "local file" in deleteFromS3
        // If we soft delete in DB (rename), do we delete file?
        // Original code: assigned deleted_ prefix in DB, AND deletedLocalFile(url).
        // I will follow original logic: delete file from disk.
        deleteLocalFile(filename);
    }

    @Override
    public AttachmentRes toggleAttachmentActiveStatus(String attachmentId) {
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        // Active status logic removed as per original comment
        return attachmentService.convertToAttachmentRes(attachment);
    }

    private String saveFileLocally(MultipartFile file) {
        try {
            Path uploadDir = Paths.get(attachmentFolderPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir.toAbsolutePath());
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = file.getName();
            }
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
            if (!fileExtension.isEmpty()) {
                uniqueFilename += "." + fileExtension;
            }

            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved successfully: {}", filePath.toAbsolutePath());

            return uniqueFilename;
        } catch (IOException e) {
            log.error("Failed to save file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Unable to save file to disk", e);
        }
    }

    private Attachment saveAttachment(MultipartFile file, String filename, String base64Data, Boolean isMain) {
        // Store only path in filePath as requested: "file_path ga faqat path o'zi
        // saqlanadi masalan: /uploads"
        // I will use attachmentFolderPath as the path.

        // Extract timestamp from filename (uuid_timestamp.ext)
        Long timestamp = extractTimestampFromFilename(filename);

        Attachment newAttachment = Attachment.builder()
                .filePath(attachmentFolderPath)
                .filename(filename)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .fileExtension(getFileExtension(file.getOriginalFilename()))
                .fileTimestamp(timestamp)
                .fileDataBase64(base64Data)
                .isActive(true)
                .isMain(isMain)
                .orderNumber(0)
                .build();
        try {
            return attachmentRepository.save(newAttachment);
        } catch (Exception e) {
            log.error("Failed to save attachment for file {}: {}", file.getOriginalFilename(), e.getMessage());
            deleteLocalFile(filename); // Rollback file upload
            throw new RuntimeException("Unable to save attachment to database", e);
        }
    }

    /**
     * Extract timestamp from filename
     * Example: "uuid-123_1768493140874.jpg" -> 1768493140874
     */
    private Long extractTimestampFromFilename(String filename) {
        try {
            // Remove extension first
            String nameWithoutExt = filename;
            int lastDotIndex = filename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                nameWithoutExt = filename.substring(0, lastDotIndex);
            }

            // Extract timestamp after last underscore
            int lastUnderscoreIndex = nameWithoutExt.lastIndexOf('_');
            if (lastUnderscoreIndex > 0 && lastUnderscoreIndex < nameWithoutExt.length() - 1) {
                String timestampStr = nameWithoutExt.substring(lastUnderscoreIndex + 1);
                return Long.parseLong(timestampStr);
            }
        } catch (Exception e) {
            log.warn("Failed to extract timestamp from filename: {}", filename);
        }
        return null;
    }

    private String convertToBase64(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            log.debug("✅ Converted file to base64 ({} bytes -> {} chars)", bytes.length, base64.length());
            return base64;
        } catch (IOException e) {
            log.warn("⚠️ Failed to convert file to base64, continuing without backup: {}", e.getMessage());
            return null;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    private void deleteLocalFile(String filename) {
        try {
            Path fileToDelete = Paths.get(attachmentFolderPath, filename);
            if (Files.exists(fileToDelete)) {
                Files.delete(fileToDelete);
                log.info("File deleted successfully: {}", fileToDelete.toAbsolutePath());
            } else {
                log.warn("File not found for deletion: {}", fileToDelete.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", filename, e.getMessage());
        }
    }
}