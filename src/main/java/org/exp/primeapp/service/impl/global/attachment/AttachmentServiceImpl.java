package org.exp.primeapp.service.impl.global.attachment;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.global.AttachmentRes;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.exp.primeapp.service.face.global.attachment.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final S3Service s3Service;
    private final AttachmentRepository attachmentRepository;

    @Value("${attachment.max.file.size.mb}")
    private Long maxFileSizeMB;

    @Override
    public void get(String attachmentUrl, HttpServletResponse response) throws IOException {
        try {
            Attachment attachment = getAttachmentWithUrl(attachmentUrl);
            if (attachment == null) {
                throw new IllegalArgumentException("Attachment not found with URL: " + attachmentUrl);
            }
            
            byte[] fileContent = getFileContent(attachmentUrl);
            String filename = attachment.getOriginalFilename() != null 
                    ? attachment.getOriginalFilename() 
                    : attachment.getFilename();

            response.setContentType(attachment.getContentType() != null ? attachment.getContentType() : "image/jpeg");
            response.setHeader("Content-Disposition", "inline; filename=\"" + (filename != null ? filename : "attachment") + "\"");
            response.getOutputStream().write(fileContent);
            response.getOutputStream().flush();
        } catch (IOException | S3Exception e) {
            log.error("Failed to fetch file for attachment URL {}: {}", attachmentUrl, e.getMessage());
            throw new IOException("Unable to retrieve file", e);
        }
    }

    private byte[] getFileContent(String url) throws IOException, S3Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Attachment URL cannot be null or empty");
        }
        return s3Service.getFile(url);
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