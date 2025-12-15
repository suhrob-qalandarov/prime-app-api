package org.exp.primeapp.controller.global.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.request.UpdateSessionReq;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.models.dto.responce.user.SessionRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.service.face.global.auth.AuthService;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.exp.primeapp.service.face.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API + V2 + AUTH)
public class BotAuthController {

    private final AuthService authService;
    private final UserService userService;
    private final SessionService sessionService;

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/me")
    public ResponseEntity<UserRes> getUserData(@AuthenticationPrincipal User user) {
        UserRes userRes = userService.getUserDataFromToken(user);
        return ResponseEntity.ok(userRes);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/admin")
    public ResponseEntity<Boolean> checkUserAdminRole(@AuthenticationPrincipal User user) {
        Boolean isAdmin = userService.getUserHasAdminFromToken(user);
        return new ResponseEntity<>(isAdmin, HttpStatus.ACCEPTED);
    }

    @PostMapping("/code/{code}")
    public ResponseEntity<LoginRes> verifyUserWithCode(
            @PathVariable Integer code, 
            HttpServletResponse response,
            HttpServletRequest request) {
        LoginRes loginRes = authService.verifyWithCodeAndSendUserData(code, response, request);
        return new ResponseEntity<>(loginRes, HttpStatus.ACCEPTED);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PostMapping("/logout")
    public ResponseEntity<LoginRes> logout(HttpServletResponse response) {
        authService.logout(response);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    // ========== Session CRUD Endpoints ==========

    @PostMapping("/session")
    public ResponseEntity<SessionRes> createSession(
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Session session = sessionService.createSessionWithToken(user, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToSessionRes(session));
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/session")
    public ResponseEntity<List<SessionRes>> getAllSessions(@AuthenticationPrincipal User user) {
        List<Session> sessions = sessionService.getAllSessionsByUser(user);
        List<SessionRes> sessionResList = sessions.stream()
                .map(this::convertToSessionRes)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessionResList);
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<SessionRes> getSessionById(@PathVariable String sessionId) {
        Session session = sessionService.getSessionById(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertToSessionRes(session));
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @PutMapping("/session/{sessionId}")
    public ResponseEntity<SessionRes> updateSession(
            @PathVariable String sessionId,
            @RequestBody UpdateSessionReq request) {
        Session existingSession = sessionService.getSessionById(sessionId);
        if (existingSession == null) {
            return ResponseEntity.notFound().build();
        }

        Session updatedSession = Session.builder()
                .ip(request.ip() != null ? request.ip() : existingSession.getIp())
                .browserInfos(request.browserInfos() != null ? request.browserInfos() : existingSession.getBrowserInfos())
                .isActive(request.isActive() != null ? request.isActive() : existingSession.getIsActive())
                .isAuthenticated(request.isAuthenticated() != null ? request.isAuthenticated() : existingSession.getIsAuthenticated())
                .build();

        Session savedSession = sessionService.updateSession(sessionId, updatedSession);
        return ResponseEntity.ok(convertToSessionRes(savedSession));
    }

    @Operation(security = @SecurityRequirement(name = "Authorization"))
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<String> deleteSession(@PathVariable String sessionId) {
        try {
            sessionService.deleteSession(sessionId);
            return ResponseEntity.ok("Session deleted successfully");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private SessionRes convertToSessionRes(Session session) {
        return SessionRes.builder()
                .sessionId(session.getSessionId())
                .ip(session.getIp())
                .browserInfos(session.getBrowserInfos() != null ? 
                        new ArrayList<>(session.getBrowserInfos()) : 
                        new ArrayList<>())
                .isActive(session.getIsActive())
                .isDeleted(session.getIsDeleted())
                .isAuthenticated(session.getIsAuthenticated())
                .isMainSession(session.getIsMainSession())
                .lastAccessedAt(session.getLastAccessedAt())
                .migratedAt(session.getMigratedAt())
                .build();
    }
}
