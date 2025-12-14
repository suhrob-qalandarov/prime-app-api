package org.exp.primeapp.service.face.global.token;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface GlobalTokenService {

    /**
     * Global token yaratish
     */
    String generateGlobalToken(String sessionId);

    /**
     * Global token ichidagi counts ni yangilash
     */
    String updateGlobalTokenCounts(String token, String countType, int increment);

    /**
     * Global token validatsiya qilish
     */
    boolean validateGlobalToken(String token);

    /**
     * Global token ichidan counts olish
     */
    Map<String, Integer> getCountsFromToken(String token);

    /**
     * Global token ichidan expiry time olish
     */
    Long getExpiryTimeFromToken(String token);

    /**
     * Global token ichidan valid time olish
     */
    Long getValidTimeFromToken(String token);

    /**
     * Cookie dan global token olish
     */
    String extractGlobalTokenFromCookie(HttpServletRequest request);

    /**
     * Cookie ga global token yozish
     */
    void setGlobalTokenCookie(String token, HttpServletResponse response, HttpServletRequest request);
}

