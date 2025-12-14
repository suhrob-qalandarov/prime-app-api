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

    @Value("${session.cookie.name:ANONYMOUS_SESSION}")
    private String sessionCookieName;

    @Value("${session.cookie.max.age:3600}")
    private Integer sessionCookieMaxAge;

    @Override
    @Transactional
    public Session getOrCreateSession(HttpServletRequest request, HttpServletResponse response) {
        // Cookie dan session ID ni olish
        String sessionId = getSessionIdFromCookie(request);
        String currentIp = ipAddressUtil.getClientIpAddress(request);
        String currentBrowserInfo = ipAddressUtil.getBrowserInfo(request);

        if (sessionId != null) {
            Session existingSession = sessionRepository.findBySessionId(sessionId)
                    .orElse(null);

            if (existingSession != null && existingSession.getIsActive() && !Boolean.TRUE.equals(existingSession.getIsDeleted())) {
                LocalDateTime now = LocalDateTime.now();
                
                // IP o'zgarishini tekshirish
                if (!currentIp.equals(existingSession.getIp())) {
                    // IP o'zgardi - yangi session yaratish (eski session ma'lumotlari bilan)
                    log.info("IP changed for session {}: {} -> {}. Creating new session.", 
                            sessionId, existingSession.getIp(), currentIp);
                    
                    // Eski session ni o'chirish
                    existingSession.setIsActive(false);
                    sessionRepository.save(existingSession);
                    
                    // Yangi session yaratish (eski session ma'lumotlari bilan)
                    Session newSession = createNewSessionWithExistingData(existingSession, currentIp, currentBrowserInfo);
                    String newSessionId = newSession.getSessionId();
                    if (response != null) {
                        setSessionCookie(newSessionId, response);
                    }
                    return newSession;
                }
                
                // IP o'zgarmagan - session ni yangilash
                existingSession.setLastAccessedAt(now);
                sessionRepository.save(existingSession);
                return existingSession;
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
                .filter(session -> session.getIsActive() && !Boolean.TRUE.equals(session.getIsDeleted()))
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
            // Check if this is user's first session
            boolean isMainSession = user.getSessions() == null || user.getSessions().isEmpty();
            
            session.setUser(user);
            session.setIsAuthenticated(true);
            session.setIsMainSession(isMainSession);
            session.setMigratedAt(LocalDateTime.now());
            session.setLastAccessedAt(LocalDateTime.now());
            sessionRepository.save(session);
            
            log.info("Session {} migrated to user {} (isMainSession: {})", sessionId, user.getId(), isMainSession);
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

        // sessionId ni database yaratadi (@GeneratedValue)
        Session session = Session.builder()
                .ip(ip)
                .browserInfo(browserInfo)
                .lastAccessedAt(now)
                .isActive(true)
                .isDeleted(false)
                .isAuthenticated(false)
                .isMainSession(false)
                .build();

        // Save qilgandan keyin database sessionId ni yaratadi
        session = sessionRepository.save(session);
        return session;
    }

    private Session createNewSessionWithExistingData(Session existingSession, String newIp, String newBrowserInfo) {
        LocalDateTime now = LocalDateTime.now();

        // Yangi session yaratish (eski session ma'lumotlari bilan)
        Session newSession = Session.builder()
                .ip(newIp)
                .browserInfo(newBrowserInfo != null ? newBrowserInfo : existingSession.getBrowserInfo())
                .user(existingSession.getUser())
                .accessToken(existingSession.getAccessToken())
                .attachmentToken(existingSession.getAttachmentToken())
                .lastAccessedAt(now)
                .isActive(true)
                .isDeleted(false)
                .isAuthenticated(existingSession.getIsAuthenticated() != null ? existingSession.getIsAuthenticated() : false)
                .isMainSession(existingSession.getIsMainSession() != null ? existingSession.getIsMainSession() : false)
                .migratedAt(existingSession.getMigratedAt())
                .build();

        // Save qilgandan keyin database sessionId ni yaratadi
        newSession = sessionRepository.save(newSession);
        return newSession;
    }
}

