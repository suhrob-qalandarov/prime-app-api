package org.exp.primeapp.service.impl.global.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.SessionRepository;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.exp.primeapp.utils.IpAddressUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final IpAddressUtil ipAddressUtil;
    private final JwtCookieService jwtCookieService;

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
                
                existingSession.setBrowserInfo(currentBrowserInfo);
                existingSession.setLastAccessedAt(now);
                sessionRepository.save(existingSession);
                return existingSession;
            }
        }

        User authenticatedUser = null;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User) {
            authenticatedUser = (User) authentication.getPrincipal();
        }
        Session newSession = createNewSession(request, authenticatedUser);
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

    private Session createNewSession(HttpServletRequest request, User user) {
        String ip = ipAddressUtil.getClientIpAddress(request);
        String browserInfo = ipAddressUtil.getBrowserInfo(request);
        LocalDateTime now = LocalDateTime.now();

        User dbUser = null;
        boolean isMainSession = false;
        if (user != null && user.getId() != null) {
            dbUser = userRepository.findById(user.getId()).orElse(null);
            if (dbUser != null) {
                List<Session> existingSessions = sessionRepository.findAllByUserIdAndIsDeletedFalse(dbUser.getId());
                isMainSession = existingSessions.isEmpty();
            }
        }

        Session session = Session.builder()
                .ip(ip)
                .browserInfo(browserInfo)
                .user(dbUser)
                .lastAccessedAt(now)
                .isActive(true)
                .isDeleted(false)
                .isAuthenticated(dbUser != null)
                .isMainSession(isMainSession)
                .build();

        session = sessionRepository.save(session);
        return session;
    }

    private Session createNewSessionWithExistingData(Session existingSession, String newIp, String newBrowserInfo) {
        LocalDateTime now = LocalDateTime.now();

        Session newSession = Session.builder()
                .ip(newIp)
                .browserInfo(newBrowserInfo)
                .user(existingSession.getUser())
                .accessToken(existingSession.getAccessToken())
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

    @Override
    @Transactional
    public Session createSession(HttpServletRequest request) {
        return createNewSession(request, null);
    }

    @Override
    public List<Session> getAllSessionsByUser(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return sessionRepository.findAllByUserIdAndIsDeletedFalse(user.getId());
    }

    @Override
    @Transactional
    public Session updateSession(String sessionId, Session updatedSession) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        
        // Faqat yangilanishi mumkin bo'lgan fieldlarni yangilash
        if (updatedSession.getIp() != null) {
            session.setIp(updatedSession.getIp());
        }
        if (updatedSession.getBrowserInfo() != null) {
            session.setBrowserInfo(updatedSession.getBrowserInfo());
        }
        if (updatedSession.getIsActive() != null) {
            session.setIsActive(updatedSession.getIsActive());
        }
        if (updatedSession.getIsAuthenticated() != null) {
            session.setIsAuthenticated(updatedSession.getIsAuthenticated());
        }
        if (updatedSession.getLastAccessedAt() != null) {
            session.setLastAccessedAt(updatedSession.getLastAccessedAt());
        }
        
        session.setLastAccessedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        
        // Agar isMain = true bo'lsa, o'chirish mumkin emas
        if (Boolean.TRUE.equals(session.getIsMainSession())) {
            throw new IllegalStateException("Main session cannot be deleted");
        }
        
        session.setIsDeleted(true);
        session.setIsActive(false);
        sessionRepository.save(session);
        log.info("Session {} marked as deleted", sessionId);
    }

    @Override
    @Transactional
    public String createSessionWithToken(User user, HttpServletRequest request, HttpServletResponse response) {
        String existingUserToken = jwtCookieService.extractTokenFromCookie(request, jwtCookieService.getCookieNameUser());
        log.debug("Checking for existing token in cookie. Token found: {}", existingUserToken != null && !existingUserToken.isBlank());
        if (existingUserToken != null && !existingUserToken.isBlank()) {
            try {
                String sessionId = jwtCookieService.getSessionIdFromToken(existingUserToken);
                if (sessionId != null) {
                    String tokenIp = jwtCookieService.getIpFromToken(existingUserToken);
                    String requestIp = ipAddressUtil.getClientIpAddress(request);
                    String tokenBrowserInfo = jwtCookieService.getBrowserInfoFromToken(existingUserToken);
                    String requestBrowserInfo = ipAddressUtil.getBrowserInfo(request);
                    
                    log.debug("Checking IP and browserInfo match: tokenIp={}, requestIp={}, tokenBrowserInfo={}, requestBrowserInfo={}", 
                            tokenIp, requestIp, tokenBrowserInfo, requestBrowserInfo);
                    
                    boolean ipMatch = tokenIp != null && tokenIp.equals(requestIp);
                    boolean browserInfoMatch = tokenBrowserInfo != null && tokenBrowserInfo.equals(requestBrowserInfo);
                    
                    if (ipMatch && browserInfoMatch) {
                        log.info("IP and browserInfo match. Returning existing token: {}", sessionId);
                        jwtCookieService.setJwtCookie(existingUserToken, jwtCookieService.getCookieNameUser(), response, request);
                        updateLastAccessed(sessionId);
                        return existingUserToken;
                    } else {
                        log.info("IP or browserInfo mismatch (tokenIp={}, requestIp={}, tokenBrowserInfo={}, requestBrowserInfo={}). Creating new session.", 
                                tokenIp, requestIp, tokenBrowserInfo, requestBrowserInfo);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get sessionId from existing token: {}. Creating new session.", e.getMessage());
            }
        }
        
        Session session = createNewSession(request, user);
        
        if (user != null) {
            String token = jwtCookieService.generateToken(user, session, request);
            setAccessToken(session.getSessionId(), token);
            jwtCookieService.setJwtCookie(token, jwtCookieService.getCookieNameUser(), response, request);
            return token;
        }
        
        String token = jwtCookieService.generateAccessTokenForAnonymous(session, request);
        setAccessToken(session.getSessionId(), token);
        jwtCookieService.setJwtCookie(token, jwtCookieService.getCookieNameUser(), response, request);
        
        return token;
    }
}

