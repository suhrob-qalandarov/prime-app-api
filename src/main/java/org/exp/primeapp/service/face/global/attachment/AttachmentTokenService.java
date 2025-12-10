package org.exp.primeapp.service.face.global.attachment;

import jakarta.servlet.http.HttpServletRequest;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.models.entities.User;

public interface AttachmentTokenService {
    
    /**
     * User authenticated bo'lsa - User dan token olish yoki yaratish
     */
    String generateTokenForUser(User user);
    
    /**
     * Session orqali token olish yoki yaratish (anonymous yoki authenticated)
     */
    String generateTokenForSession(Session session);
    
    /**
     * Token validation (user talab qilmaydi)
     */
    boolean validateToken(String token);
    
    /**
     * Token refresh
     */
    String refreshToken(String oldToken, HttpServletRequest request);
    
    /**
     * Token expiry tekshirish (kunlarda)
     */
    long getTokenExpiryDays(String token);
}

