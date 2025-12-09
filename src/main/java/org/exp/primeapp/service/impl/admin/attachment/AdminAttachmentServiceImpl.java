package org.exp.primeapp.service.impl.admin.attachment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.global.AttachmentRes;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.service.face.admin.attachment.AdminAttachmentService;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAttachmentServiceImpl implements AdminAttachmentService {

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
        // TODO: Implement local file storage
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
                    // TODO: Implement local file storage
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
        // TODO: Implement local file storage
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
        String url = attachment.getUrl();

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

        // TODO: Implement local file deletion
        deleteLocalFile(url);
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

    private String saveFileLocally(MultipartFile file) {
        // TODO: Implement local file storage
        // For now, return a placeholder URL
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : file.getName();
        return "/uploads/" + System.currentTimeMillis() + "_" + filename;
    }

    private Attachment saveAttachment(MultipartFile file, String url) {
        String filePath = url; // TODO: Update when implementing local file storage
        Attachment newAttachment = Attachment.builder()
                .url(url)
                .filePath(filePath)
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
        // TODO: Implement local file deletion
        log.debug("File deletion not yet implemented for URL: {}", url);
    }
}