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
                messageService.removeKeyboardAndSendMsg(chatId);
                userService.updateUserPhoneById(chatId, contact.phoneNumber());
                messageService.sendCode(user);

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
            try {
                // Try to send error message to user
                Long chatId = message.chat().id();
                messageService.sendStartMsg(chatId, "Foydalanuvchi");
            } catch (Exception ex) {
                log.error("Failed to send error message to chatId: {}", message.chat().id(), ex);
            }
        }
    }
}
