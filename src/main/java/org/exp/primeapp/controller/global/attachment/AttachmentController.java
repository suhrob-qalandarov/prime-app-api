package org.exp.primeapp.controller.global.attachment;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@MultipartConfig
@RequiredArgsConstructor
@RequestMapping(API + V1 + ATTACHMENT)
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * Attachment olish - public endpoint (ochiq)
     */
    @Operation(summary = "Get attachment by URL")
    @GetMapping("/{url}")
    public ResponseEntity<?> getAttachment(
            @PathVariable String url,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // log.debug("Fetching attachment with URL: {}", url); // Maybe too noisy?
            attachmentService.get(url, request, response);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to fetch attachment with URL: {}", url, e);
            throw new RuntimeException("Failed to fetch attachment: " + e.getMessage(), e);
        }
    }
}
