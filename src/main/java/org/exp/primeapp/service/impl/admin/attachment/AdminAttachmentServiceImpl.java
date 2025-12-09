package org.exp.primeapp.service.impl.admin.attachment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.global.AttachmentRes;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.service.face.admin.attachment.AdminAttachmentService;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.exp.primeapp.service.face.global.attachment.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAttachmentServiceImpl implements AdminAttachmentService {

    private final S3Service s3Service;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;

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
        String key = uploadToS3(file);
        return saveAttachment(file, key);
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
                    String key = uploadToS3(file);
                    return saveAttachment(file, key);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public AttachmentRes update(Long attachmentId, MultipartFile file) {
        attachmentService.validateFile(file);
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        String oldKey = attachment.getUrl();
        String newKey = uploadToS3(file);

        attachment.setUrl(newKey);
        attachment.setFilename(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());

        try {
            Attachment saved = attachmentRepository.save(attachment);
            if (oldKey != null && !oldKey.equals(newKey)) {
                deleteOldS3File(oldKey);
            }
            return attachmentService.convertToAttachmentRes(saved);
        } catch (Exception e) {
            log.error("Failed to update attachment ID {} in database: {}", attachmentId, e.getMessage());
            s3Service.deleteFile(newKey); // Rollback new S3 upload
            throw new RuntimeException("Unable to update attachment in database", e);
        }
    }

    @Transactional
    @Override
    public void delete(Long attachmentId) {
        Attachment attachment = attachmentService.getAttachment(attachmentId);

        try {
            attachment.setActive(false);
            attachmentRepository.save(attachment);
        } catch (Exception e) {
            log.error("Failed to soft-delete attachment ID {}: {}", attachmentId, e.getMessage());
            throw new RuntimeException("Unable to soft-delete attachment in database", e);
        }
    }

    @Transactional
    @Override
    public void deleteFromS3(Long attachmentId) {
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        String key = attachment.getUrl();

        if (attachment.getActive() || attachment.getUrl().startsWith("deleted_")) {
            return;
        }

        try {
            attachment.setUrl("deleted_" + attachment.getUrl());
            attachment.setActive(false);
            attachmentRepository.save(attachment);
        } catch (Exception e) {
            log.error("Failed to soft-delete attachment ID {}: {}", attachmentId, e.getMessage());
            throw new RuntimeException("Unable to soft-delete attachment in database", e);
        }

        deleteOldS3File(key);
    }

    @Override
    public AttachmentRes toggleAttachmentActiveStatus(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found with ID: " + attachmentId));
        Boolean active = attachment.getActive();

        if (active) {
            attachment.setActive(false);
            log.info("Attachment deactivated {}", attachment.getId());
        } else {
            attachment.setActive(true);
            log.info("Attachment activated {}", attachment.getId());
        }

        Attachment updatedAttachment = attachmentRepository.save(attachment);
        return attachmentService.convertToAttachmentRes(updatedAttachment);
    }

    private String uploadToS3(MultipartFile file) {
        try {
            return s3Service.uploadAttachment(file);
        } catch (IOException e) {
            log.error("Failed to upload file {} to S3: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Unable to upload file to S3", e);
        } catch (S3Exception e) {
            log.error("S3 error while uploading file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("S3 service error during file upload", e);
        }
    }

    private Attachment saveAttachment(MultipartFile file, String key) {
        Attachment newAttachment = Attachment.builder()
                .url(key)
                .filePath(key) // TODO: Update when implementing local file storage
                .filename(file.getOriginalFilename())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .fileExtension(getFileExtension(file.getOriginalFilename()))
                .active(true)
                .build();
        try {
            return attachmentRepository.save(newAttachment);
        } catch (Exception e) {
            log.error("Failed to save attachment for file {}: {}", file.getOriginalFilename(), e.getMessage());
            s3Service.deleteFile(key); // Rollback S3 upload
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

    private void deleteOldS3File(String key) {
        try {
            s3Service.deleteFile(key);
        } catch (S3Exception e) {
            log.warn("Failed to delete S3 file with key {}: {}", key, e.getMessage());
        }
    }
}