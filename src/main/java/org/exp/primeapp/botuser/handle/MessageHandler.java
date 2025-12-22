package org.exp.primeapp.botuser.handle;

import com.pengrad.telegrambot.model.Contact;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botuser.service.interfaces.MessageService;
import org.exp.primeapp.botuser.service.interfaces.UserService;
import org.exp.primeapp.models.entities.User;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler implements Consumer<Message> {

    private final MessageService messageService;
    private final UserService userService;

    @Override
    public void accept(Message message) {
        try {
            log.debug("Received message from chatId: {}, text: {}", message.chat().id(), message.text());
            
            String text = message.text();
            User user = userService.getOrCreateUser(message.from());
            Long userId = user.getId();
            Long chatId = user.getTelegramId();
            
            log.debug("Processing message for user: {} (chatId: {}), text: {}", userId, chatId, text);

            if (message.contact() != null) {
                Contact contact = message.contact();
                log.info("Received contact from chatId: {}, phone: {}", chatId, contact.phoneNumber());
                messageService.removeKeyboardAndSendMsg(chatId);
                userService.updateUserPhoneById(chatId, contact.phoneNumber());
                // User obyektini yangilash - telefon raqami yangilanganidan keyin
                User updatedUser = userService.getOrCreateUser(message.from());
                log.info("User phone updated, sending code to chatId: {}", chatId);
                messageService.sendCode(updatedUser);

            } else if (text != null && text.trim().startsWith("/start")) {
                log.info("Processing /start command from chatId: {}", chatId);
                String firstName = user.getFirstName() != null ? user.getFirstName() : "Foydalanuvchi";
                
                // Check if user has phone number (already registered)
                boolean hasPhone = user.getPhone() != null && !user.getPhone().trim().isEmpty();
                
                if (hasPhone) {
                    // User already has phone - send code page
                    log.info("User {} already has phone, sending code page", userId);
                    messageService.sendCode(user);
                } else {
                    // Regular user without phone - ask for contact
                    messageService.sendStartMsg(chatId, firstName);
                }
                log.info("Start message sent to chatId: {}", chatId);

            } else if (text != null && text.trim().equals("/login")) {
                log.info("Processing /login command from chatId: {}", chatId);
                userService.updateOneTimeCode(chatId);
                messageService.sendLoginMsg(chatId);
            } else {
                log.debug("No handler for message from chatId: {}, text: {}", chatId, text);
            }
        } catch (Exception e) {
            log.error("Error processing message from chatId: {}", message.chat().id(), e);
            // Don't send start message on error - it might confuse users
            // Just log the error and let the user try again
        }
    }
}
