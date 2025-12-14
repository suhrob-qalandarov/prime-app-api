package org.exp.primeapp.service.face.global.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.models.entities.User;

public interface SessionService {
    
    /**
     * Session yaratish yoki olish (cookie orqali)
     */
    Session getOrCreateSession(HttpServletRequest request, HttpServletResponse response);
    
    /**
     * Session ID orqali session olish
     */
    Session getSessionById(String sessionId);
    
    /**
     * Session ga access token biriktirish
     */
    void setAccessToken(String sessionId, String accessToken);
    
    /**
     * Session ni user ga biriktirish (migration)
     */
    void migrateSessionToUser(String sessionId, User user);
    
    /**
     * Session ni yangilash (lastAccessedAt)
     */
    void updateLastAccessed(String sessionId);
    
    /**
     * Session dan access token olish
     */
    String getAccessToken(String sessionId);
    
    /**
     * Cookie dan session ID olish
     */
    String getSessionIdFromCookie(HttpServletRequest request);
    
    /**
     * Cookie ga session ID yozish
     */
    void setSessionCookie(String sessionId, HttpServletResponse response);
    
    /**
     * Session yaratish
     */
    Session createSession(HttpServletRequest request);
    
    /**
     * Barcha sessionlarni olish (user uchun)
     */
    java.util.List<Session> getAllSessionsByUser(User user);
    
    /**
     * Session ni yangilash
     */
    Session updateSession(String sessionId, Session session);
    
    /**
     * Session ni o'chirish (isDeleted = true)
     * Agar isMain = true bo'lsa, o'chirish mumkin emas
     */
    void deleteSession(String sessionId);
}

