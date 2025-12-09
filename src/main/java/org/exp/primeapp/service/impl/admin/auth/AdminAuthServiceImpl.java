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
import org.exp.primeapp.models.entities.UserIpInfo;
import org.exp.primeapp.repository.UserIpInfoRepository;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.admin.auth.AdminAuthService;
import org.exp.primeapp.utils.IpAddressUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserRepository userRepository;
    private final UserIpInfoRepository userIpInfoRepository;
    private final JwtCookieService jwtService;
    private final IpAddressUtil ipAddressUtil;

    @Value("${cookie.max.age}")
    private Integer cookieMaxAge;

    @Value("${cookie.name.admin}")
    private String cookieNameAdmin;

    @Override
    @Transactional
    public LoginRes checkAdminLogin(AdminLoginReq loginReq, HttpServletResponse response, HttpServletRequest request) {
        User u = userRepository.findByPhoneAndVerifyCode(loginReq.phoneNumber(), loginReq.verifyCode())
                .orElseThrow();

        // IP va browser ma'lumotlarini olish va saqlash
        String ip = ipAddressUtil.getClientIpAddress(request);
        String browserInfo = ipAddressUtil.getBrowserInfo(request);
        
        // Agar register IP bo'lmasa, saqlash
        boolean hasRegisterInfo = userIpInfoRepository.findByUserIdAndIsRegisterInfoTrue(u.getId()).isPresent();
        if (!hasRegisterInfo) {
            UserIpInfo registerInfo = UserIpInfo.builder()
                    .user(u)
                    .ip(ip)
                    .browserInfo(browserInfo)
                    .accessedAt(LocalDateTime.now())
                    .isRegisterInfo(true)
                    .build();
            userIpInfoRepository.save(registerInfo);
        }
        
        // Login IP ni saqlash (agar allaqachon yo'q bo'lsa)
        boolean ipExists = userIpInfoRepository.existsByUserIdAndIpAndBrowserInfo(u.getId(), ip, browserInfo);
        
        if (!ipExists) {
            UserIpInfo userIpInfo = UserIpInfo.builder()
                    .user(u)
                    .ip(ip)
                    .browserInfo(browserInfo)
                    .accessedAt(LocalDateTime.now())
                    .isRegisterInfo(false)
                    .build();
            userIpInfoRepository.save(userIpInfo);
        }
        
        String token = jwtService.generateToken(u);
        userRepository.save(u);

        jwtService.setJwtCookie(token, cookieNameAdmin, response);

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
