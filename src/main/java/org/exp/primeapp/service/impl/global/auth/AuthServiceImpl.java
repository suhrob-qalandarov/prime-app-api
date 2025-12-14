package org.exp.primeapp.service.impl.global.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.models.dto.responce.order.UserProfileOrdersRes;
import org.exp.primeapp.models.dto.responce.user.SessionRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.service.face.global.auth.AuthService;
import org.exp.primeapp.service.face.global.attachment.AttachmentTokenService;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.exp.primeapp.service.face.user.OrderService;
import org.exp.primeapp.utils.UserUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtCookieService jwtService;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final SessionService sessionService;
    private final AttachmentTokenService attachmentTokenService;
    private final UserUtil userUtil;

    @Value("${cookie.max.age}")
    private Integer cookieMaxAge;

    @Value("${cookie.name.user}")
    private String cookieNameUser;

    @Value("${cookie.name.admin}")
    private String cookieNameAdmin;

    @Transactional
    @Override
    public LoginRes verifyWithCodeAndSendUserData(Integer code, HttpServletResponse response, HttpServletRequest request) {
        User user = userRepository.findOneByVerifyCode(code);

        if (user == null) {
            throw new IllegalArgumentException("Code noto'g'ri");
        }

        if (user.getVerifyCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Code expired");
        }
        
        // Session topish yoki yaratish
        Session session = sessionService.getOrCreateSession(request, response);
        
        // Session ni user ga biriktirish (migration)
        if (session.getUser() == null) {
            sessionService.migrateSessionToUser(session.getSessionId(), user);
            // Session ni qayta olish (migration dan keyin yangilanadi)
            session = sessionService.getSessionById(session.getSessionId());
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
                token = jwtService.generateToken(user, session, request);
                sessionService.setAccessToken(session.getSessionId(), token);
            }
        } else {
            // Token yo'q - yangi yaratish
            token = jwtService.generateToken(user, session, request);
            sessionService.setAccessToken(session.getSessionId(), token);
        }
        
        // Attachment token yaratish yoki olish
        String attachmentToken = sessionService.getAttachmentToken(session.getSessionId());
        if (attachmentToken == null || !attachmentTokenService.validateToken(attachmentToken)) {
            attachmentToken = attachmentTokenService.generateTokenForSession(session);
        }
        
        userRepository.save(user);

        jwtService.setJwtCookie(token, cookieNameUser, response, request);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserProfileOrdersRes profileOrdersById = orderService.getUserProfileOrdersById(user.getId());

        // Convert sessions to SessionRes
        List<SessionRes> sessions = user.getSessions() != null ? user.getSessions().stream()
                .map(s -> SessionRes.builder()
                        .sessionId(s.getSessionId())
                        .ip(s.getIp())
                        .browserInfo(s.getBrowserInfo())
                        .isActive(s.getIsActive())
                        .isDeleted(s.getIsDeleted())
                        .isAuthenticated(s.getIsAuthenticated())
                        .isMainSession(s.getIsMainSession())
                        .lastAccessedAt(s.getLastAccessedAt())
                        .migratedAt(s.getMigratedAt())
                        .build())
                .toList() : List.of();

        UserRes userRes = UserRes.builder()
                .id(user.getId())
                .firstName(userUtil.truncateName(user.getFirstName()))
                .lastName(userUtil.truncateName(user.getLastName()))
                .phone(user.getPhone())
                .username(user.getTgUsername())
                //.roles(user.getRoles().stream().map(Role::getName).toList())
                .orders(profileOrdersById)
                .sessions(sessions)
                .isAdmin(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_VISITOR")))
                .isVisitor(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_VISITOR")))
                .isSuperAdmin(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_SUPER_ADMIN")))
                .build();

        long nowMillis = System.currentTimeMillis();
        long expiryMillis = nowMillis + cookieMaxAge * 1000L;

        return LoginRes.builder()
                .token(token)
                .user(userRes)
                .expiryMillis(expiryMillis)
                .build();
    }

    @Override
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieNameUser, null);
        cookie.setHttpOnly(true);
        //cookie.setDomain("howdy.uz");
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);

        Cookie cookieAdmin = new Cookie(cookieNameAdmin, null);
        cookieAdmin.setHttpOnly(true);
        //cookieAdmin.setDomain("howdy.uz");
        cookieAdmin.setSecure(true);
        cookieAdmin.setPath("/");
        cookieAdmin.setMaxAge(0);
        cookieAdmin .setAttribute("SameSite", "None");
        response.addCookie(cookieAdmin );
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