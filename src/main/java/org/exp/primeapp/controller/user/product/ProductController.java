package org.exp.primeapp.controller.user.product;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.dto.responce.user.ProductPageRes;
import org.exp.primeapp.models.dto.responce.user.ProductRes;
import org.exp.primeapp.models.dto.responce.user.page.PageRes;
import org.exp.primeapp.models.entities.Session;
import org.exp.primeapp.service.face.global.session.SessionService;
import org.exp.primeapp.service.face.user.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@RequestMapping(API + V1 + PRODUCT)
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final JwtCookieService jwtCookieService;
    private final SessionService sessionService;

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(
            @PathVariable Long productId,
            HttpServletRequest request,
            HttpServletResponse response) {
        return handleSessionTokenRequest("product", request, response, () -> {
            ProductRes product = productService.getProductById(productId);
            return ResponseEntity.ok(product);
        });
    }

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) String spotlightName,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String colorName,
            @RequestParam(required = false) String sizeName,
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) String sortBy, // "discount", "low-price", "high-price"
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            HttpServletResponse response) {
        return handleSessionTokenRequest("product", request, response, () -> {
            Pageable pageable = PageRequest.of(page, size);
            PageRes<ProductPageRes> pageableProducts = productService.getActiveProducts(
                    spotlightName, categoryName, colorName, sizeName, brandName, sortBy, pageable);
            return ResponseEntity.ok(pageableProducts);
        });
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable Long categoryId,
            Pageable pageable,
            HttpServletRequest request,
            HttpServletResponse response) {
        return handleSessionTokenRequest("product", request, response, () -> {
            PageRes<ProductRes> pageableProducts = productService.getProductsByCategoryId(categoryId, pageable);
            return ResponseEntity.ok(pageableProducts);
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
