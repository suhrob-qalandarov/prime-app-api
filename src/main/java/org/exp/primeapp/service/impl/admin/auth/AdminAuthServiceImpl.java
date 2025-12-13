package org.exp.primeapp.service.impl.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.dto.request.AdminLoginReq;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.admin.auth.AdminAuthService;
import org.exp.primeapp.service.face.global.attachment.AttachmentTokenService;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.exp.primeapp.utils.UserUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserRepository userRepository;
    private final JwtCookieService jwtService;
    private final SessionService sessionService;
    private final AttachmentTokenService attachmentTokenService;
    private final UserUtil userUtil;

    @Value("${cookie.max.age}")
    private Integer cookieMaxAge;

    @Value("${cookie.name.admin}")
    private String cookieNameAdmin;

    @Override
    @Transactional
    public LoginRes checkAdminLogin(AdminLoginReq loginReq, HttpServletResponse response, HttpServletRequest request) {
        User u = userRepository.findByPhoneAndVerifyCode(loginReq.phoneNumber(), loginReq.verifyCode())
                .orElseThrow();

        // Session topish yoki yaratish
        Session session = sessionService.getOrCreateSession(request, response);
        
        // Session ni user ga biriktirish (migration)
        if (session.getUser() == null) {
            sessionService.migrateSessionToUser(session.getSessionId(), u);
        }
        
        // Access token yaratish yoki olish
        String existingAccessToken = sessionService.getAccessToken(session.getSessionId());
        String token;
        
        if (existingAccessToken != null) {
            // Token expiry tekshirish (3 kun)
            long expiryDays = getAccessTokenExpiryDays(existingAccessToken);
            if (expiryDays >= 3) {
                // 3 kun va undan oshiq - eski token ishlatiladi
                token = existingAccessToken;
            } else {
                // 3 kundan kam - yangi token yaratiladi
                token = jwtService.generateToken(u);
                sessionService.setAccessToken(session.getSessionId(), token);
            }
        } else {
            // Token yo'q - yangi yaratish
            token = jwtService.generateToken(u);
            sessionService.setAccessToken(session.getSessionId(), token);
        }
        
        // Attachment token yaratish yoki olish
        String attachmentToken = sessionService.getAttachmentToken(session.getSessionId());
        if (attachmentToken == null || !attachmentTokenService.validateToken(attachmentToken)) {
            attachmentToken = attachmentTokenService.generateTokenForSession(session);
        }
        
        userRepository.save(u);

        jwtService.setJwtCookie(token, cookieNameAdmin, response, request);

        var auth = new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserRes userRes = UserRes.builder()
                .id(u.getId())
                .firstName(userUtil.truncateName(u.getFirstName()))
                .lastName(userUtil.truncateName(u.getLastName()))
                .phone(u.getPhone())
                .username(u.getTgUsername())
                .isAdmin(u.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("ROLE_ADMIN")))
                .isVisitor(u.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("ROLE_VISITOR")))
                .isSuperAdmin(u.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("ROLE_SUPER_ADMIN")))
                .build();

        long nowMillis = System.currentTimeMillis();
        long expiryMillis = nowMillis + cookieMaxAge * 1000L;

        return LoginRes.builder()
                .token(token)
                .user(userRes)
                .expiryMillis(expiryMillis)
                .build();
    }
    
    private long getAccessTokenExpiryDays(String token) {
        try {
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                    .verifyWith(jwtService.getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            java.util.Date expiration = claims.getExpiration();
            if (expiration == null) {
                return 0;
            }

            LocalDateTime expiryDateTime = LocalDateTime.ofInstant(
                    expiration.toInstant(),
                    java.time.ZoneId.systemDefault()
            );
            LocalDateTime now = LocalDateTime.now();

            return java.time.temporal.ChronoUnit.DAYS.between(now, expiryDateTime);
        } catch (Exception e) {
            log.warn("Failed to get access token expiry: {}", e.getMessage());
            return 0;
        }
    }
}
