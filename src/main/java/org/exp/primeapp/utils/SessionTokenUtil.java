package org.exp.primeapp.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Session token count yangilash va tekshirish uchun util class
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTokenUtil {

    private final JwtCookieService jwtCookieService;
    private final SessionService sessionService;

    /**
     * Session token bilan ishlash - count tekshirish va yangilash
     * 
     * @param countType "product" yoki "category"
     * @param request HTTP request
     * @param response HTTP response
     * @param successHandler Muvaffaqiyatli bo'lganda ishlatiladigan handler
     * @return ResponseEntity
     */
    public ResponseEntity<?> handleSessionTokenRequest(
            String countType,
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<ResponseEntity<?>> successHandler) {
        
        // Session token olish
        String token = jwtCookieService.extractTokenFromCookie(request);
        
        // Agar token yo'q bo'lsa, yangi session yaratish
        if (token == null) {
            Session session = sessionService.getOrCreateSession(request, response);
            token = jwtCookieService.generateAccessTokenForAnonymous(session, request);
            sessionService.setAccessToken(session.getSessionId(), token);
            jwtCookieService.setJwtCookie(token, jwtCookieService.getCookieNameUser(), response, request);
        }
        
        // Count olish
        Integer count = countType.equals("product") 
                ? jwtCookieService.getProductCount(token)
                : jwtCookieService.getCategoryCount(token);
        
        if (count > 0) {
            // Count > 0: count--, token yangilash, deliver
            try {
                String sessionId = jwtCookieService.getSessionIdFromToken(token);
                Session session = sessionService.getSessionById(sessionId);
                if (session == null) {
                    // Session topilmasa, yangi yaratish
                    session = sessionService.getOrCreateSession(request, response);
                }
                
                String newToken = jwtCookieService.updateTokenWithDecrementedCount(token, countType, request, session);
                jwtCookieService.setJwtCookie(newToken, jwtCookieService.getCookieNameUser(), response, request);
                sessionService.setAccessToken(session.getSessionId(), newToken);
                
                // successHandler.get() exception tashlasa, GlobalExceptionHandler handle qiladi
                return successHandler.get();
            } catch (jakarta.persistence.EntityNotFoundException | org.springframework.web.server.ResponseStatusException e) {
                // EntityNotFoundException va ResponseStatusException GlobalExceptionHandler ga o'tkazish
                throw e;
            } catch (Exception e) {
                // Faqat session token bilan bog'liq exception'larni catch qilish
                log.error("Error in handleSessionTokenRequest (count > 0): {}", e.getMessage(), e);
                throw new RuntimeException("Session token processing failed: " + e.getMessage(), e);
            }
        } else {
            // Count = 0
            boolean expired = jwtCookieService.isDataExpired(token);
            
            if (expired) {
                // Expired: yangi token (count = -1), deliver
                try {
                    String sessionId = jwtCookieService.getSessionIdFromToken(token);
                    Session session = sessionService.getSessionById(sessionId);
                    if (session == null) {
                        session = sessionService.getOrCreateSession(request, response);
                    }
                    
                    String newToken = jwtCookieService.generateNewTokenWithCount(countType, -1, session, request);
                    jwtCookieService.setJwtCookie(newToken, jwtCookieService.getCookieNameUser(), response, request);
                    sessionService.setAccessToken(session.getSessionId(), newToken);
                    
                    // successHandler.get() exception tashlasa, GlobalExceptionHandler handle qiladi
                    return successHandler.get();
                } catch (jakarta.persistence.EntityNotFoundException | org.springframework.web.server.ResponseStatusException e) {
                    // EntityNotFoundException va ResponseStatusException GlobalExceptionHandler ga o'tkazish
                    throw e;
                } catch (Exception e) {
                    // Faqat session token bilan bog'liq exception'larni catch qilish
                    log.error("Error in handleSessionTokenRequest (expired): {}", e.getMessage(), e);
                    throw new RuntimeException("Session token processing failed: " + e.getMessage(), e);
                }
            } else {
                // Valid: 418 I'm a teapot response
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
            }
        }
    }
}

