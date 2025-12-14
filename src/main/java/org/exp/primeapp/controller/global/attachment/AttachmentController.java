package org.exp.primeapp.controller.global.attachment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@MultipartConfig
@RequiredArgsConstructor
@RequestMapping(API + V1 + ATTACHMENT)
public class AttachmentController {

    private final AttachmentService attachmentService;

    // Endi globalToken cookie da bo'ladi, alohida endpoint kerak emas
    // GlobalToken cookie dan olinadi va ishlatiladi

    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, String>> refreshAttachmentToken(
            @RequestParam String token,
            HttpServletRequest request) {
        String newToken = attachmentService.refreshAttachmentToken(token, request);
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    /**
     * Attachment olish - public endpoint (token bilan himoyalangan)
     */
    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/{url}")
    public void getAttachment(
            @PathVariable String url,
            @RequestParam String token,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        log.debug("Fetching attachment with URL: {} and token", url);
        attachmentService.get(url, token, request, response);
        log.info("Successfully served attachment: {}", url);
    }
}
