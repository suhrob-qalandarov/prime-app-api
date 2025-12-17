package org.exp.primeapp.controller.user.category;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.dto.responce.user.CategoryRes;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.exp.primeapp.service.face.user.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequestMapping(API + V1 + CATEGORY)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final JwtCookieService jwtCookieService;
    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<?> getCategories(
            HttpServletRequest request,
            HttpServletResponse response) {
        return handleSessionTokenRequest("category", request, response, () -> {
            List<CategoryRes> categories = categoryService.getResCategories();
            return ResponseEntity.ok(categories);
        });
    }

    @GetMapping("/{spotlightName}")
    public ResponseEntity<?> getCategoriesBySpotlightName(
            @PathVariable String spotlightName,
            HttpServletRequest request,
            HttpServletResponse response) {
        return handleSessionTokenRequest("category", request, response, () -> {
            List<CategoryRes> categories = categoryService.getResCategoriesBySpotlightName(spotlightName);
            return ResponseEntity.ok(categories);
        });
    }
    
    /**
     * Session token bilan ishlash - count tekshirish va yangilash
     */
    private ResponseEntity<?> handleSessionTokenRequest(
            String countType,
            HttpServletRequest request,
            HttpServletResponse response,
            java.util.function.Supplier<ResponseEntity<?>> successHandler) {
        
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
                    session = sessionService.getOrCreateSession(request, response);
                }
                
                String newToken = jwtCookieService.updateTokenWithDecrementedCount(token, countType, request, session);
                jwtCookieService.setJwtCookie(newToken, jwtCookieService.getCookieNameUser(), response, request);
                sessionService.setAccessToken(session.getSessionId(), newToken);
                
                return successHandler.get();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
                    
                    return successHandler.get();
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                // Valid: 418 I'm a teapot response
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
            }
        }
    }

    /*@GetMapping("/by-spotlight/{spotlightId}")
    public ResponseEntity <List<CategoryRes>> getCategoriesBySpotlightId(@PathVariable Long spotlightId) {
        List<CategoryRes> categories = categoryService.getSpotlightCategories(spotlightId);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }*/
}
