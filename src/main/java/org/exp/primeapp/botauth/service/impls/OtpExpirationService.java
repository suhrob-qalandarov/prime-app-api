package org.exp.primeapp.botauth.service.impls;

import com.pengrad.telegrambot.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botauth.service.interfaces.MessageService;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OtpExpirationService {

    private final UserRepository userRepository;
    private final MessageService messageService;
    private final TelegramBot telegramBot;

    @Autowired(required = false)
    public OtpExpirationService(UserRepository userRepository,
                               MessageService messageService,
                               TelegramBot telegramBot) {
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.telegramBot = telegramBot;
    }

    // Har 30 soniyada tekshirish
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void deleteExpiredOtpMessages() {
        if (telegramBot == null) {
            return; // Bot disabled bo'lsa, ishlamaydi
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            List<User> usersWithExpiredCodes = userRepository.findUsersWithExpiredOtpCodes(now);

            if (usersWithExpiredCodes.isEmpty()) {
                return;
            }

            log.info("Found {} users with expired OTP codes. Deleting messages...", usersWithExpiredCodes.size());

            for (User user : usersWithExpiredCodes) {
                if (user.getTelegramId() != null && user.getMessageId() != null) {
                    try {
                        messageService.deleteOtpMessage(user.getTelegramId(), user.getMessageId());

                        // Message ID ni null qilish
                        user.setMessageId(null);
                        userRepository.save(user);
                    } catch (Exception e) {
                        log.error("Error deleting OTP message for user {}: {}", user.getId(), e.getMessage());
                    }
                }
            }

            log.info("✅ Completed deleting expired OTP messages");
        } catch (Exception e) {
            log.error("❌ Error in scheduled task for deleting expired OTP messages: {}", e.getMessage(), e);
        }
    }
}

