package org.exp.primeapp.controller.global.attachment;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getAttachmentToken() {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must be authenticated to generate attachment token");
        }
        String token = attachmentService.generateAttachmentToken(user);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, String>> refreshAttachmentToken(@RequestParam String token) {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must be authenticated to refresh attachment token");
        }
        String newToken = attachmentService.refreshAttachmentToken(token, user);
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @GetMapping("/{url}")
    public void getAttachment(
            @PathVariable String url,
            @RequestParam String token,
            HttpServletResponse response) throws IOException {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must be authenticated to access attachments");
        }
        
        if (!attachmentService.validateAttachmentToken(token, user)) {
            throw new IllegalArgumentException("Invalid or expired attachment token");
        }
        
        log.debug("Fetching attachment with URL: {} and token", url);
        attachmentService.get(url, token, response);
        log.info("Successfully served attachment: {}", url);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        
        if (authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            return userRepository.findById(user.getId()).orElse(null);
        }
        
        return null;
    }
}
