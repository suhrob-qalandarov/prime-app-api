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
import org.exp.primeapp.service.face.global.attachment.AttachmentTokenService;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentTokenService attachmentTokenService;
    private final SessionService sessionService;

    @Value("${attachment.max.file.size.mb}")
    private Long maxFileSizeMB;

    @Override
    public void get(String attachmentUrl, String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Token validation
        if (!attachmentTokenService.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired attachment token");
            return;
        }

        // Session lastAccessedAt yangilash
        String sessionId = sessionService.getSessionIdFromCookie(request);
        if (sessionId != null) {
            sessionService.updateLastAccessed(sessionId);
        }

        try {
            Attachment attachment = getAttachmentWithUrl(attachmentUrl);
            if (attachment == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            byte[] fileContent = getFileContent(attachmentUrl);
            String filename = attachment.getOriginalFilename() != null 
                    ? attachment.getOriginalFilename() 
                    : attachment.getFilename();

            response.setContentType(attachment.getContentType() != null ? attachment.getContentType() : "image/jpeg");
            response.setHeader("Content-Disposition", "inline; filename=\"" + (filename != null ? filename : "attachment") + "\"");
            response.setContentLength(fileContent.length);
            response.getOutputStream().write(fileContent);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("Failed to fetch file for attachment URL {}: {}", attachmentUrl, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new IOException("Unable to retrieve file", e);
        }
    }

    @Override
    public String generateAttachmentToken(org.exp.primeapp.models.entities.User user) {
        return attachmentTokenService.generateTokenForUser(user);
    }

    @Override
    public String refreshAttachmentToken(String oldToken, HttpServletRequest request) {
        return attachmentTokenService.refreshToken(oldToken, request);
    }

    @Override
    public boolean validateAttachmentToken(String token) {
        return attachmentTokenService.validateToken(token);
    }

    private byte[] getFileContent(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Attachment URL cannot be null or empty");
        }
        // TODO: Implement local file reading from filePath
        throw new UnsupportedOperationException("File reading from local storage not yet implemented");
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