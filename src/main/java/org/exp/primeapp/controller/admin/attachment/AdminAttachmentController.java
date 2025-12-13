package org.exp.primeapp.controller.admin.attachment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.global.AttachmentRes;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.service.face.admin.attachment.AdminAttachmentService;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(API + V1 + ADMIN + ATTACHMENT)
public class AdminAttachmentController {

    private final AdminAttachmentService adminAttachmentService;
    private final AttachmentService attachmentService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VISITOR')")
    public ResponseEntity<Attachment> getAttachment(@PathVariable Long attachmentId) {
        log.debug("Fetching attachment with ID: {}", attachmentId);
        return ResponseEntity.ok(attachmentService.getAttachment(attachmentId));
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/with-url/{attachmentUrl}")
    public ResponseEntity<Attachment> getAttachmentWithUrl(@PathVariable String attachmentUrl) {
        log.debug("Fetching attachment with URL: {}", attachmentUrl);
        return ResponseEntity.ok(attachmentService.getAttachmentWithUrl(attachmentUrl));
    }

    @Operation(
            summary = "Upload single file",
            description = "Upload a single file attachment",
            security = @SecurityRequirement(name = "Authorization"),
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "object")
                    ),
                    required = true
            )
    )
    @PostMapping(value = "/oneupload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttachmentRes> uploadFile(
            @Parameter(
                    description = "File to upload",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data", schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("file") MultipartFile file) {
        log.debug("Uploading single file: {}", file.getOriginalFilename());
        Attachment attachment = adminAttachmentService.uploadOne(file);
        AttachmentRes response = AttachmentRes.builder()
                .id(attachment.getId())
                .url(attachment.getUrl())
                .filename(attachment.getFilename())
                .originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .fileExtension(attachment.getFileExtension())
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Upload multiple files",
            description = "Upload multiple file attachments",
            security = @SecurityRequirement(name = "Authorization"),
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "object")
                    ),
                    required = true
            )
    )
    @PostMapping(value = "/multiupload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> uploadFiles(
            @Parameter(
                    description = "Files to upload",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data", schema = @Schema(type = "array", format = "binary"))
            )
            @RequestParam("files") MultipartFile[] files) {
        log.debug("Uploading multiple files: {}", files.length);
        List<Attachment> uploadedFiles = adminAttachmentService.uploadMultiple(files);
        List<String> responses = attachmentService.convertToAttachmentUrls(uploadedFiles);
        return ResponseEntity.ok(responses);
    }


    @Operation(
            summary = "Update attachment file",
            description = "Update an existing attachment file",
            security = @SecurityRequirement(name = "Authorization"),
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "object")
                    ),
                    required = true
            )
    )
    @PutMapping(value = "/{attachmentId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttachmentRes> updateFile(
            @Parameter(description = "Attachment ID", required = true)
            @PathVariable Long attachmentId,
            @Parameter(
                    description = "File to upload",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data", schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("file") MultipartFile file) {
        log.debug("Updating attachment ID: {}", attachmentId);
        AttachmentRes response = adminAttachmentService.update(attachmentId, file);
        return ResponseEntity.ok(response);
    }

    /*@PostMapping("/toggle/{attachmentId}")
    public ResponseEntity<AttachmentRes> activate(@PathVariable Long attachmentId) {
        log.debug("Updating attachment ID: {}", attachmentId);
        AttachmentRes response = adminAttachmentService.toggleAttachmentActiveStatus(attachmentId);
        return ResponseEntity.ok(response);
    }*/

    /*@DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        log.debug("Deleting attachment ID: {}", attachmentId);
        adminAttachmentService.delete(attachmentId);
        return ResponseEntity.noContent().build();
    }*/

    /*@DeleteMapping("/delete-from-base/{attachmentId}")
    public ResponseEntity<Void> deleteAttachmentFromS3(@PathVariable Long attachmentId) {
        log.debug("Deleting attachment ID: {}", attachmentId);
        adminAttachmentService.deleteFromS3(attachmentId);
        return ResponseEntity.noContent().build();
    }*/
}