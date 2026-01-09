package org.exp.primeapp.service.impl.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.dto.request.AdminLoginReq;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.admin.auth.AdminAuthService;
import org.exp.primeapp.utils.IpAddressUtil;
import org.exp.primeapp.utils.UserUtil;
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
        private final UserUtil userUtil;
        private final IpAddressUtil ipAddressUtil;

        @Value("${cookie.max.age}")
        private Integer cookieMaxAge;

        @Value("${cookie.name.admin}")
        private String cookieNameAdmin;

        @Override
        @Transactional
        public LoginRes checkAdminLogin(AdminLoginReq loginReq, HttpServletResponse response,
                        HttpServletRequest request) {
                User u = userRepository.findByPhoneAndVerifyCode(loginReq.phoneNumber(), loginReq.verifyCode())
                                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.UNAUTHORIZED,
                                                "Invalid phone number or verification code"));

                // Update User info (IP and Browser)
                String clientIp = ipAddressUtil.getClientIpAddress(request);
                String browserInfo = request.getHeader("User-Agent");

                u.setIp(clientIp);
                u.setBrowserInfo(browserInfo);

                // Generate token
                String token = jwtService.generateToken(u, request);

                userRepository.save(u);

                jwtService.setJwtCookie(token, cookieNameAdmin, response, request);

                var auth = new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);

                UserRes userRes = UserRes.builder()
                                .id(u.getId())
                                .firstName(userUtil.truncateName(u.getFirstName()))
                                .lastName(userUtil.truncateName(u.getLastName()))
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
