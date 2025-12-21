package org.exp.primeapp.botauth.service.impls;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.botauth.service.interfaces.ButtonService;
import org.exp.primeapp.botauth.service.interfaces.MessageService;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final TelegramBot telegramBot;
    private final ButtonService buttonService;
    private final UserServiceImpl botAuthUserService;
    private final UserRepository userRepository;

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
    public void sendStartMsgForAdmin(Long chatId, String firstName) {
        try {
            if (telegramBot == null) {
                log.error("❌ Telegram bot is null! Cannot send start message to chatId: {}", chatId);
                return;
            }
            
            log.info("Sending start message for admin to chatId: {}, firstName: {}", chatId, firstName);
            
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
            log.error("❌ Exception while sending start message for admin to chatId: {}", chatId, e);
        }
    }

    @Override
    public void sendAdminMenu(Long chatId, String firstName) {
        try {
            if (telegramBot == null) {
                log.error("❌ Telegram bot is null! Cannot send admin menu to chatId: {}", chatId);
                return;
            }
            
            log.info("Sending admin menu to chatId: {}, firstName: {}", chatId, firstName);
            
            SendMessage sendMessage = new SendMessage(chatId,
                    "👨‍💼 <b>Xush kelibsiz, Admin!</b>\n\n" +
                    "Quyidagi bo'limlardan birini tanlang:")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createAdminMainMenuButtons());
            
            SendResponse response = telegramBot.execute(sendMessage);
            
            if (response.isOk()) {
                log.info("✅ Admin menu sent successfully to chatId: {}", chatId);
            } else {
                log.error("❌ Failed to send admin menu to chatId: {}. Error: {} (errorCode: {})", 
                        chatId, response.description(), response.errorCode());
            }
        } catch (Exception e) {
            log.error("❌ Exception while sending admin menu to chatId: {}", chatId, e);
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
        Integer oneTimeCode = botAuthUserService.generateOneTimeCode();
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
        Integer oneTimeCode = botAuthUserService.generateOneTimeCode();
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
        Integer oneTimeCode = botAuthUserService.generateOneTimeCode();
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

    @Override
    public void sendProductCreationStart(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "🛍️ <b>Yangi mahsulot qo'shish</b>\n\n" +
                "Mahsulot qo'shish jarayonini boshlaymiz. Quyidagi ma'lumotlarni ketma-ket kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductNamePrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📝 <b>1/8</b> Mahsulot nomini kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductDescriptionPrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📝 <b>2/8</b> Mahsulot tavsifini kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductBrandPrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "🏷️ <b>3/8</b> Brend nomini kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductImagePrompt(Long chatId, int currentCount) {
        String message = "📷 <b>4/8</b> Mahsulot rasmlarini yuboring:\n\n";
        message += "• Minimum: 1 ta rasm\n";
        message += "• Maksimum: 3 ta rasm\n";
        message += "• Hozirgi: " + currentCount + " ta";
        
        telegramBot.execute(new SendMessage(chatId, message)
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendImageSavedSuccess(Long chatId, int currentCount, int remaining) {
        String message = "✅ Rasm muvaffaqiyatli saqlandi!\n";
        message += "Qo'shish mumkin yana " + remaining + " ta rasm!";
        
        telegramBot.execute(new SendMessage(chatId, message)
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createNextStepImageButton())
        );
    }

    @Override
    public void sendImagesCompleted(Long chatId, int totalCount) {
        String message = "✅ Mahsulot rasmlari muvaffaqiyatli saqlandi!\n";
        message += "Hozirgi: " + totalCount + " ta";
        
        telegramBot.execute(new SendMessage(chatId, message)
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendSpotlightNamePromptForProduct(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📂 <b>5/8</b> Toifani tanlang:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createSpotlightNameButtons())
        );
    }

    @Override
    public void sendCategorySelection(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📂 <b>6/8</b> Kategoriyani tanlang:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendSizeSelection(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📏 <b>7/8</b> O'lchamlarni tanlang (bir nechtasini tanlash mumkin):")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductConfirmation(Long chatId, String productInfo) {
        telegramBot.execute(new SendMessage(chatId,
                "✅ <b>8/8</b> Mahsulot ma'lumotlari:\n\n" + productInfo +
                "\n\nMahsulotni qo'shishni tasdiqlaysizmi?")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createConfirmationButtons())
        );
    }

    @Override
    public void sendProductSavedSuccess(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "✅ Mahsulot muvaffaqiyatli qo'shildi!")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductCreationCancelled(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "❌ Mahsulot qo'shish bekor qilindi.")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductSizeQuantityPrompt(Long chatId, org.exp.primeapp.botauth.models.ProductCreationState state) {
        StringBuilder prompt = new StringBuilder("📊 Har bir o'lcham uchun miqdorni kiriting:\n\n");
        
        for (org.exp.primeapp.models.enums.Size size : state.getSelectedSizes()) {
            Integer currentQty = state.getSizeQuantities().getOrDefault(size, 0);
            if (currentQty == 0) {
                prompt.append("• <b>").append(size.getLabel()).append("</b>: miqdorni kiriting (raqam)\n");
            } else {
                prompt.append("• <b>").append(size.getLabel()).append("</b>: ✅ ").append(currentQty).append(" ta\n");
            }
        }
        
        telegramBot.execute(new SendMessage(chatId, prompt.toString())
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendCategoryCreationStart(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📂 <b>Yangi kategoriya qo'shish</b>\n\n" +
                "Kategoriya qo'shish jarayonini boshlaymiz. Quyidagi ma'lumotlarni kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendCategoryNamePrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📝 <b>1/2</b> Kategoriya nomini kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendSpotlightNamePrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📂 <b>2/2</b> Spotlight nomini tanlang:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createSpotlightNameButtons())
        );
    }

    @Override
    public void sendCategoryConfirmation(Long chatId, String categoryInfo) {
        telegramBot.execute(new SendMessage(chatId,
                "✅ <b>Kategoriya ma'lumotlari:</b>\n\n" + categoryInfo +
                "\n\nKategoriyani qo'shishni tasdiqlaysizmi?")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createCategoryConfirmationButtons())
        );
    }

    @Override
    public void sendCategorySavedSuccess(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "✅ Kategoriya muvaffaqiyatli qo'shildi!")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendCategoryCreationCancelled(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "❌ Kategoriya qo'shish bekor qilindi.")
                .parseMode(ParseMode.HTML)
        );
    }
}
