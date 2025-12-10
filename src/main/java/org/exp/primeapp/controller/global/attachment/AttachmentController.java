package org.exp.primeapp.controller.global.attachment;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.global.attachment.AttachmentService;
import org.exp.primeapp.service.face.global.attachment.AttachmentTokenService;
import org.exp.primeapp.service.face.global.session.SessionService;
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
    private final AttachmentTokenService attachmentTokenService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    /**
     * Anonymous user uchun token olish
     */
    @GetMapping("/token/anonymous")
    public ResponseEntity<Map<String, String>> getAnonymousAttachmentToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        // Session yaratish yoki olish
        Session session = sessionService.getOrCreateSession(request, response);
        
        // Session dan attachment token olish yoki yaratish
        String token = sessionService.getAttachmentToken(session.getSessionId());
        if (token == null || !attachmentTokenService.validateToken(token)) {
            token = attachmentTokenService.generateTokenForSession(session);
        }
        
        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * Authenticated user uchun token olish
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getAttachmentToken(HttpServletRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must be authenticated to generate attachment token");
        }
        
        // Session yaratish yoki olish
        Session session = sessionService.getOrCreateSession(request, null);
        
        // Session dan attachment token olish yoki yaratish
        String token = sessionService.getAttachmentToken(session.getSessionId());
        if (token == null || !attachmentTokenService.validateToken(token)) {
            token = attachmentTokenService.generateTokenForSession(session);
        }
        
        return ResponseEntity.ok(Map.of("token", token));
    }

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
