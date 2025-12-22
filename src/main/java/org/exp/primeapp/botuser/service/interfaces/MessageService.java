package org.exp.primeapp.botuser.service.interfaces;

import org.exp.primeapp.models.entities.User;

public interface MessageService {

    void sendStartMsg(Long chatId, String firstName);

    void sendLoginMsg(Long chatId);

    void removeKeyboardAndSendCode(User user);

    void removeKeyboardAndSendMsg(Long telegramId);

    void sendCode(User user);

    void renewCode(User user);

    void deleteOtpMessage(Long chatId, Integer messageId);
}
