package org.exp.primeapp.service.impl.global.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.configs.security.JwtCookieService;
import org.exp.primeapp.models.dto.responce.global.LoginRes;
import org.exp.primeapp.models.dto.responce.order.UserProfileOrdersRes;
import org.exp.primeapp.models.dto.responce.user.UserRes;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.service.face.global.auth.AuthService;
import org.exp.primeapp.service.face.user.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtCookieService jwtService;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Value("${cookie.max.age}")
    private Integer cookieMaxAge;

    @Value("${cookie.name.user}")
    private String cookieNameUser;

    @Value("${cookie.name.admin}")
    private String cookieNameAdmin;

    @Transactional
    @Override
    public LoginRes verifyWithCodeAndSendUserData(Integer code, HttpServletResponse response) {
        User user = userRepository.findOneByVerifyCode(code);

        if (user == null) {
            throw new IllegalArgumentException("Code noto‘g‘ri");
        }

        if (user.getVerifyCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Code expired");
        }
        String token = jwtService.generateToken(user);

        jwtService.setJwtCookie(token, cookieNameUser, response);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserProfileOrdersRes profileOrdersById = orderService.getUserProfileOrdersById(user.getId());

        UserRes userRes = UserRes.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .phone(user.getPhone())
                .username(user.getTgUsername())
                //.roles(user.getRoles().stream().map(Role::getName).toList())
                .orders(profileOrdersById)
                .isAdmin(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_VISITOR")))
                .build();

        long nowMillis = System.currentTimeMillis();
        long expiryMillis = nowMillis + cookieMaxAge * 1000L;

        return LoginRes.builder()
                .token(token)
                .user(userRes)
                .expiryMillis(expiryMillis)
                .build();
    }

    @Override
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieNameUser, null);
        cookie.setHttpOnly(true);
        //cookie.setDomain("howdy.uz");
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);

        Cookie cookieAdmin = new Cookie(cookieNameAdmin, null);
        cookieAdmin.setHttpOnly(true);
        //cookieAdmin.setDomain("howdy.uz");
        cookieAdmin.setSecure(true);
        cookieAdmin.setPath("/");
        cookieAdmin.setMaxAge(0);
        cookieAdmin .setAttribute("SameSite", "None");
        response.addCookie(cookieAdmin );
    }
}