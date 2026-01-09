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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAttachmentServiceImpl implements AdminAttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;

    @Value("${app.attachment.base.url}")
    private String attachmentBaseUrl;

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
        String url = saveFileLocally(file);
        return saveAttachment(file, url);
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
                    String url = saveFileLocally(file);
                    return saveAttachment(file, url);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public AttachmentRes update(Long attachmentId, MultipartFile file) {
        attachmentService.validateFile(file);
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        String oldUrl = attachment.getUrl();
        String newUrl = saveFileLocally(file);

        attachment.setUrl(newUrl);
        attachment.setFilename(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());

        try {
            Attachment saved = attachmentRepository.save(attachment);
            if (oldUrl != null && !oldUrl.equals(newUrl)) {
                deleteLocalFile(oldUrl);
            }
            return attachmentService.convertToAttachmentRes(saved);
        } catch (Exception e) {
            log.error("Failed to update attachment ID {} in database: {}", attachmentId, e.getMessage());
            deleteLocalFile(newUrl); // Rollback new file upload
            throw new RuntimeException("Unable to update attachment in database", e);
        }
    }

    @Transactional
    @Override
    public void delete(Long attachmentId) {
        Attachment attachment = attachmentService.getAttachment(attachmentId);

        try {
            attachmentRepository.delete(attachment);
        } catch (Exception e) {
            log.error("Failed to delete attachment ID {}: {}", attachmentId, e.getMessage());
            throw new RuntimeException("Unable to delete attachment in database", e);
        }
    }

    @Transactional
    @Override
    public void deleteFromS3(Long attachmentId) {
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        String url = attachment.getUrl();

        if (attachment.getUrl().startsWith("deleted_")) {
            return;
        }

        try {
            attachment.setUrl("deleted_" + attachment.getUrl());
            attachmentRepository.save(attachment);
        } catch (Exception e) {
            log.error("Failed to soft-delete attachment ID {}: {}", attachmentId, e.getMessage());
            throw new RuntimeException("Unable to soft-delete attachment in database", e);
        }

        deleteLocalFile(url);
    }

    @Override
    public AttachmentRes toggleAttachmentActiveStatus(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found with ID: " + attachmentId));

        log.info("Toggle active status called for attachment {}, but active field is removed.", attachment.getId());

        return attachmentService.convertToAttachmentRes(attachment);
    }

    private String saveFileLocally(MultipartFile file) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(attachmentFolderPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir.toAbsolutePath());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = file.getName();
            }
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
            if (!fileExtension.isEmpty()) {
                uniqueFilename += "." + fileExtension;
            }

            // Save file to disk
            Path filePath = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved successfully: {}", filePath.toAbsolutePath());

            // Generate URL: base_url + folder_path + filename
            String baseUrl = attachmentBaseUrl.endsWith("/")
                    ? attachmentBaseUrl.substring(0, attachmentBaseUrl.length() - 1)
                    : attachmentBaseUrl;
            String folderPath = attachmentFolderPath.startsWith("/")
                    ? attachmentFolderPath
                    : "/" + attachmentFolderPath;
            String url = baseUrl + folderPath + "/" + uniqueFilename;

            return url;
        } catch (IOException e) {
            log.error("Failed to save file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Unable to save file to disk", e);
        }
    }

    private Attachment saveAttachment(MultipartFile file, String url) {
        // Extract file path from URL (relative path)
        String filePath = url.replace(attachmentBaseUrl, "");
        if (!filePath.startsWith("/")) {
            filePath = "/" + filePath;
        }

        Attachment newAttachment = Attachment.builder()
                .url(url)
                .filePath(filePath)
                .filename(file.getOriginalFilename())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .fileExtension(getFileExtension(file.getOriginalFilename()))
                .build();
        try {
            return attachmentRepository.save(newAttachment);
        } catch (Exception e) {
            log.error("Failed to save attachment for file {}: {}", file.getOriginalFilename(), e.getMessage());
            deleteLocalFile(url); // Rollback file upload
            throw new RuntimeException("Unable to save attachment to database", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    private void deleteLocalFile(String url) {
        try {
            // Extract file path from URL
            String baseUrl = attachmentBaseUrl.endsWith("/")
                    ? attachmentBaseUrl.substring(0, attachmentBaseUrl.length() - 1)
                    : attachmentBaseUrl;
            String filePath = url.replace(baseUrl, "");
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }

            Path fileToDelete = Paths.get(filePath);
            if (Files.exists(fileToDelete)) {
                Files.delete(fileToDelete);
                log.info("File deleted successfully: {}", fileToDelete.toAbsolutePath());
            } else {
                log.warn("File not found for deletion: {}", fileToDelete.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to delete file with URL {}: {}", url, e.getMessage());
            // Don't throw exception - file deletion failure shouldn't break the flow
        }
    }
}