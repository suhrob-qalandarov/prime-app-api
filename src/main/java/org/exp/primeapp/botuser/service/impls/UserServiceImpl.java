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
import java.util.Optional;

@Service("botUserService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    @Override
    public User getOrCreateUser(com.pengrad.telegrambot.model.User tgUser) {
        Long id = tgUser.id();
        Optional<User> userOptional = userRepository.findByTelegramId(id);

        if (userOptional.isPresent()) {
            return userOptional.get();
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
        // Telefon raqamidan + ni olib tashlash
        String formattedPhone = formatPhoneNumber(phoneNumber);
        userRepository.updatePhoneByUserId(userId, formattedPhone);
    }
    
    /**
     * Telefon raqamini format qilish: + ni olib tashlash
     * @param phoneNumber Telefon raqami
     * @return Formatlangan telefon raqami (+ siz)
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }
        
        String trimmed = phoneNumber.trim();
        
        // Agar + bilan boshlansa, uni olib tashlash
        if (trimmed.startsWith("+")) {
            return trimmed.substring(1);
        }
        
        return trimmed;
    }

    @Override
    public Integer generateOneTimeCode() {
        SecureRandom secureRandom = new SecureRandom();
        // Generate a 6-digit code
        return 100000 + secureRandom.nextInt(900000);
    }
}