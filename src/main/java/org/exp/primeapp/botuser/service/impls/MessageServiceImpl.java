package org.exp.primeapp.botuser.service.impls;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import org.exp.primeapp.botuser.service.interfaces.ButtonService;
import org.exp.primeapp.botuser.service.interfaces.MessageService;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    private final TelegramBot telegramBot;
    private final ButtonService buttonService;
    private final UserServiceImpl botUserService;
    private final UserRepository userRepository;

    public MessageServiceImpl(@Qualifier("userBot") TelegramBot telegramBot,
                              ButtonService buttonService,
                              UserServiceImpl botUserService,
                              UserRepository userRepository) {
        this.telegramBot = telegramBot;
        this.buttonService = buttonService;
        this.botUserService = botUserService;
        this.userRepository = userRepository;
    }

    @Override
    public void sendStartMsg(Long chatId, String firstName) {
        try {
            if (telegramBot == null) {
                log.error("❌ Telegram bot is null! Cannot send start message to chatId: {}", chatId);
                return;
            }
            
            log.info("Sending start message to chatId: {}, firstName: {}", chatId, firstName);
            
            SendMessage sendMessage = new SendMessage(chatId,
                    """
                            🇺🇿
                            Salom """ + firstName + "👋\n" +
                            """ 
                                    @prime77uz'ning rasmiy botiga xush kelibsiz
                                    
                                    ⬇ Kontaktingizni yuboring (tugmani bosib)
                                    """
            )
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.sendShareContactBtn());
            
            SendResponse response = telegramBot.execute(sendMessage);
            
            if (response.isOk()) {
                log.info("✅ Start message sent successfully to chatId: {}", chatId);
            } else {
                log.error("❌ Failed to send start message to chatId: {}. Error: {} (errorCode: {})", 
                        chatId, response.description(), response.errorCode());
            }
        } catch (Exception e) {
            log.error("❌ Exception while sending start message to chatId: {}", chatId, e);
        }
    }


    @Override
    public void sendLoginMsg(Long chatId) {
        telegramBot.execute(new SendMessage(chatId, """
                🇺🇿
                🔑 Yangi kod olish uchun /login ni bosing""")
                .parseMode(ParseMode.Markdown)
        );
    }

    @Override
    public void removeKeyboardAndSendCode(User user) {
        Integer oneTimeCode = botUserService.generateOneTimeCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(2);
        telegramBot.execute(new SendMessage(user.getTelegramId(),
                "🔒 Kod:\n<pre>" + oneTimeCode + "</pre>"
        )
                .parseMode(ParseMode.HTML)
                .replyMarkup(new ReplyKeyboardRemove())
        );
        userRepository.updateVerifyCodeAndExpiration(user.getTelegramId(), oneTimeCode, expirationTime);
        //sendLoginMsg(user.getTelegramId());
    }

    @Override
    public void removeKeyboardAndSendMsg(Long chatId) {
        telegramBot.execute(new SendMessage(chatId, "🔑Muvaffaqiyatli ro'yhatdan o'tkazildi!")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(new ReplyKeyboardRemove())
        );
    }

    @Transactional
    public void sendCode(User user) {
        Integer oneTimeCode = botUserService.generateOneTimeCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(5);
        SendResponse response = telegramBot.execute(new SendMessage(user.getTelegramId(),
                        "🔒 Kod: <pre>" + oneTimeCode + "</pre>" + "\n\n\uD83D\uDD17 Bosing va Kiring: \nprime/login"
                )
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.sendRenewCodeBtn())
        );
        userRepository.updateVerifyCodeAndExpiration(user.getTelegramId(), oneTimeCode, expirationTime);
        userRepository.updateMessageId(user.getTelegramId(), response.message().messageId());
        //sendLoginMsg(user.getTelegramId());
    }

    @Transactional
    public void renewCode(User user) {
        Integer oneTimeCode = botUserService.generateOneTimeCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(5);

        SendResponse response = (SendResponse) telegramBot.execute(new EditMessageText(
                user.getTelegramId(),
                user.getMessageId(),
                "🔒 Kod: \n<pre>" + oneTimeCode + "</pre>" + "\n\n\uD83D\uDD17 Bosing va Kiring: \nprime/login"
                ).parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.sendRenewCodeBtn())
        );
        userRepository.updateVerifyCodeAndExpiration(user.getTelegramId(), oneTimeCode, expirationTime);
        userRepository.updateMessageId(user.getTelegramId(), response.message().messageId());
    }

    @Override
    public void deleteOtpMessage(Long chatId, Integer messageId) {
        if (telegramBot == null || chatId == null || messageId == null) {
            return;
        }

        try {
            var deleteRequest = new DeleteMessage(chatId, messageId);
            var response = telegramBot.execute(deleteRequest);
            if (response.isOk()) {
                log.info("✅ OTP message deleted successfully for chatId: {}, messageId: {}", chatId, messageId);
            } else {
                log.warn("⚠️ Failed to delete OTP message for chatId: {}, messageId: {}. Error: {}",
                        chatId, messageId, response.description());
            }
        } catch (Exception e) {
            log.error("❌ Error deleting OTP message for chatId: {}, messageId: {}", chatId, messageId, e);
        }
    }

}
