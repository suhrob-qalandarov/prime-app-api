package org.exp.primeapp.service.impl.admin.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.dto.request.AdminLoginReq;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.admin.auth.AdminAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserRepository userRepository;
    private final JwtCookieService jwtService;

    @Value("${cookie.max.age}")
    private Integer cookieMaxAge;

    @Override
    public LoginRes checkAdminLogin(AdminLoginReq loginReq, HttpServletResponse response) {
        User u = userRepository.findByPhoneAndVerifyCode(loginReq.phoneNumber(), loginReq.verifyCode())
                .orElseThrow();

        String token = jwtService.generateToken(u);

        jwtService.setJwtCookie(token, "prime-admin-token", response);

        var auth = new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserRes userRes = UserRes.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .phone(u.getPhone())
                .username(u.getTgUsername())
                .isAdmin(u.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("ROLE_ADMIN")))
                .isVisitor(u.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("ROLE_VISITOR")))
                .isSuperAdmin(u.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("ROLE_SUPER_ADMIN")))
                .build();

        long nowMillis = System.currentTimeMillis();
        long expiryMillis = nowMillis + cookieMaxAge * 1000L;

        return LoginRes.builder()
                .token(token)
                .user(userRes)
                .expiryMillis(expiryMillis)
                .build();
    }
}
