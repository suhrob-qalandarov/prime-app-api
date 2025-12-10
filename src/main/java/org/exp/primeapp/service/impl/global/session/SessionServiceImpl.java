package org.exp.primeapp.service.impl.global.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.SessionRepository;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.exp.primeapp.utils.IpAddressUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final IpAddressUtil ipAddressUtil;

    @Value("${session.expiry.minutes:60}")
    private Integer sessionExpiryMinutes;

    @Value("${session.cookie.name:ANONYMOUS_SESSION}")
    private String sessionCookieName;

    @Value("${session.cookie.max.age:3600}")
    private Integer sessionCookieMaxAge;

    @Override
    @Transactional
    public Session getOrCreateSession(HttpServletRequest request, HttpServletResponse response) {
        // Cookie dan session ID ni olish
        String sessionId = getSessionIdFromCookie(request);

        if (sessionId != null) {
            Session existingSession = sessionRepository.findBySessionId(sessionId)
                    .orElse(null);

            if (existingSession != null && existingSession.getIsActive()) {
                LocalDateTime now = LocalDateTime.now();
                
                // Session expired tekshirish
                if (existingSession.getExpiresAt().isAfter(now)) {
                    // Session ni yangilash
                    existingSession.setLastAccessedAt(now);
                    sessionRepository.save(existingSession);
                    return existingSession;
                } else {
                    // Session expired, yangi yaratish
                    existingSession.setIsActive(false);
                    sessionRepository.save(existingSession);
                }
            }
        }

        // Yangi session yaratish (database sessionId ni yaratadi)
        Session newSession = createNewSession(request);
        // Save qilgandan keyin sessionId ni olish va cookie ga yozish
        String newSessionId = newSession.getSessionId();
        if (response != null) {
            setSessionCookie(newSessionId, response);
        }
        return newSession;
    }

    @Override
    public Session getSessionById(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        return sessionRepository.findBySessionId(sessionId)
                .filter(session -> session.getIsActive() && session.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    @Override
    public Session findSessionByIpAndBrowser(String ip, String browserInfo) {
        LocalDateTime now = LocalDateTime.now();
        return sessionRepository.findByIpAndBrowserInfo(ip, browserInfo, now)
                .orElse(null);
    }

    @Override
    @Transactional
    public void setAccessToken(String sessionId, String accessToken) {
        Session session = getSessionById(sessionId);
        if (session != null) {
            session.setAccessToken(accessToken);
            session.setLastAccessedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    @Override
    @Transactional
    public void setAttachmentToken(String sessionId, String attachmentToken) {
        Session session = getSessionById(sessionId);
        if (session != null) {
            session.setAttachmentToken(attachmentToken);
            session.setLastAccessedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    @Override
    @Transactional
    public void migrateSessionToUser(String sessionId, User user) {
        Session session = getSessionById(sessionId);
        if (session != null && user != null) {
            session.setUser(user);
            session.setMigratedAt(LocalDateTime.now());
            session.setLastAccessedAt(LocalDateTime.now());
            sessionRepository.save(session);
            
            log.info("Session {} migrated to user {}", sessionId, user.getId());
        }
    }

    @Override
    @Transactional
    public void updateLastAccessed(String sessionId) {
        Session session = getSessionById(sessionId);
        if (session != null) {
            session.setLastAccessedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    @Override
    public String getAttachmentToken(String sessionId) {
        Session session = getSessionById(sessionId);
        return session != null ? session.getAttachmentToken() : null;
    }

    @Override
    public String getAccessToken(String sessionId) {
        Session session = getSessionById(sessionId);
        return session != null ? session.getAccessToken() : null;
    }

    @Override
    public String getSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (sessionCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void setSessionCookie(String sessionId, HttpServletResponse response) {
        Cookie cookie = new Cookie(sessionCookieName, sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(sessionCookieMaxAge);
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);
    }

    private Session createNewSession(HttpServletRequest request) {
        String ip = ipAddressUtil.getClientIpAddress(request);
        String browserInfo = ipAddressUtil.getBrowserInfo(request);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(sessionExpiryMinutes);

        // sessionId ni database yaratadi (@GeneratedValue)
        Session session = Session.builder()
                .ip(ip)
                .browserInfo(browserInfo)
                .expiresAt(expiresAt)
                .lastAccessedAt(now)
                .isActive(true)
                .build();

        // Save qilgandan keyin database sessionId ni yaratadi
        session = sessionRepository.save(session);
        return session;
    }
}

