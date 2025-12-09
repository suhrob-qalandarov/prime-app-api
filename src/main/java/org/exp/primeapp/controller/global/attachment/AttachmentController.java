package org.exp.primeapp.controller.global.attachment;

import jakarta.servlet.annotation.MultipartConfig;
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

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getAttachmentToken() {
        String token = attachmentService.generateAttachmentToken();
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, String>> refreshAttachmentToken(@RequestParam String token) {
        String newToken = attachmentService.refreshAttachmentToken(token);
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @GetMapping("/{url}")
    public void getAttachment(
            @PathVariable String url,
            @RequestParam String token,
            HttpServletResponse response) throws IOException {
        log.debug("Fetching attachment with URL: {} and token", url);
        attachmentService.get(url, token, response);
        log.info("Successfully served attachment: {}", url);
    }
}
