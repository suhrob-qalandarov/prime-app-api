package org.exp.primeapp.botuser.service.impls;

import lombok.RequiredArgsConstructor;
import org.exp.primeapp.botuser.service.interfaces.UserService;
import org.exp.primeapp.models.entities.Role;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.RoleRepository;
import org.exp.primeapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service("botUserService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

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