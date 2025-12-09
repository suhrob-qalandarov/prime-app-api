package org.exp.primeapp.botauth.service.impls;

import lombok.RequiredArgsConstructor;
import org.exp.primeapp.botauth.service.interfaces.UserService;
import org.exp.primeapp.models.entities.Role;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.models.entities.UserIpInfo;
import org.exp.primeapp.repository.RoleRepository;
import org.exp.primeapp.repository.UserIpInfoRepository;
import org.exp.primeapp.repository.UserRepository;
import org.exp.primeapp.utils.IpAddressUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service("botAuthUserService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserIpInfoRepository userIpInfoRepository;
    private final RoleRepository roleRepository;
    private final IpAddressUtil ipAddressUtil;

    @Transactional
    @Override
    public User getOrCreateUser(com.pengrad.telegrambot.model.User tgUser) {
        Long id = tgUser.id();
        User user = userRepository.findByTelegramId(id);

        if (user != null) {
            return user;
        }

        List<Role> roleUser = roleRepository.findALlByNameIn(List.of("ROLE_USER"));

        // Generate one-time code and set expiration (2 minutes from now)
        Integer oneTimeCode = generateOneTimeCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(2);

        // IP va browser ma'lumotlarini olish (agar request mavjud bo'lsa)
        String ip = "Telegram Bot";
        String browserInfo = "Telegram Bot";
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
            ip = ipAddressUtil.getClientIpAddress(request);
            browserInfo = ipAddressUtil.getBrowserInfo(request);
        }

        User build = User.builder()
                .telegramId(tgUser.id())
                .firstName(tgUser.firstName())
                .lastName(tgUser.lastName())
                .tgUsername(tgUser.username())
                .active(true)
                .verifyCode(oneTimeCode)
                .verifyCodeExpiration(expirationTime)
                .roles(roleUser)
                .build();
        
        User savedUser = userRepository.save(build);
        
        // Register info yaratish va saqlash
        UserIpInfo registerInfo = UserIpInfo.builder()
                .user(savedUser)
                .ip(ip)
                .browserInfo(browserInfo)
                .accessedAt(LocalDateTime.now())
                .isRegisterInfo(true)
                .build();
        userIpInfoRepository.save(registerInfo);
        
        return savedUser;
    }

    @Override
    public void updateOneTimeCode(Long userId) {
        Integer oneTimeCode = generateOneTimeCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(2);
        userRepository.updateVerifyCodeAndExpiration(userId, oneTimeCode, expirationTime);
    }


    @Override
    public void updateTgUser(Long userId, User user) {

    }

    @Override
    public void updateUserPhoneById(Long userId, String phoneNumber) {
        userRepository.updatePhoneByUserId(userId, phoneNumber);
    }

    @Override
    public Integer generateOneTimeCode() {
        SecureRandom secureRandom = new SecureRandom();
        // Generate a 6-digit code
        return 100000 + secureRandom.nextInt(900000);
    }
}