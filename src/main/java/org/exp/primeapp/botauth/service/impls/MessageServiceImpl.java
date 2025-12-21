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
import org.exp.primeapp.models.entities.Role;
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
                    "Quyidagi bo'limlardan birini tanlang👇")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createAdminMainReplyKeyboard());
            
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
    public void sendAdminMenuWithCancel(Long chatId) {
        try {
            if (telegramBot == null) {
                log.error("❌ Telegram bot is null! Cannot send admin menu to chatId: {}", chatId);
                return;
            }
            
            SendMessage sendMessage = new SendMessage(chatId,
                    "👨‍💼 <b>Xush kelibsiz, Admin!</b>\n\n" +
                    "Quyidagi bo'limlardan birini tanlang👇")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createAdminMainReplyKeyboard());
            
            telegramBot.execute(sendMessage);
        } catch (Exception e) {
            log.error("❌ Exception while sending admin menu to chatId: {}", chatId, e);
        }
    }
    
    @Override
    public void sendAdminSectionMessage(Long chatId, String sectionName) {
        try {
            if (telegramBot == null) {
                return;
            }
            
            SendMessage sendMessage;
            
            if (sectionName.equals("Mahsulotlar")) {
                // Send inline keyboard message
                SendMessage inlineMessage = new SendMessage(chatId,
                        "🛍️ <b>Mahsulotlar bo'limi</b>\n\nQuyidagi amallardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createProductMenuButtons());
                telegramBot.execute(inlineMessage);
                
                // Send reply keyboard "Bekor qilish" button
                SendMessage cancelMessage = new SendMessage(chatId, " ")
                        .replyMarkup(buttonService.createAdminCancelReplyKeyboard());
                telegramBot.execute(cancelMessage);
                return;
            } else if (sectionName.equals("Kategoriyalar")) {
                // Send inline keyboard message
                SendMessage inlineMessage = new SendMessage(chatId,
                        "📂 <b>Kategoriyalar bo'limi</b>\n\nQuyidagi amallardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createCategoryMenuButtons());
                telegramBot.execute(inlineMessage);
                
                // Send reply keyboard "Bekor qilish" button
                SendMessage cancelMessage = new SendMessage(chatId, " ")
                        .replyMarkup(buttonService.createAdminCancelReplyKeyboard());
                telegramBot.execute(cancelMessage);
                return;
            } else {
                sendMessage = new SendMessage(chatId,
                        "📂 <b>" + sectionName + "</b> bo'limi")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createAdminCancelReplyKeyboard());
            }
            
            telegramBot.execute(sendMessage);
        } catch (Exception e) {
            log.error("❌ Exception while sending admin section message to chatId: {}", chatId, e);
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
                "📝 <b>1/9</b> Mahsulot nomini kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductDescriptionPrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📝 <b>2/9</b> Mahsulot tavsifini kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductBrandPrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "🏷️ <b>3/9</b> Brend nomini kiriting:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductImagePrompt(Long chatId, int currentCount) {
        String message = "📷 <b>4/9</b> Mahsulot rasmlarini yuboring:\n\n";
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
                "📂 <b>5/9</b> Toifani tanlang:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createSpotlightNameButtons())
        );
    }

    @Override
    public void sendCategorySelection(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📂 <b>6/9</b> Kategoriyani tanlang:")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendSizeSelection(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📏 <b>7/9</b> O'lchamlarni tanlang (bir nechtasini tanlash mumkin):")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductPricePrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "💰 <b>8/9</b> Mahsulot narxini kiriting (so'm):")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendProductConfirmation(Long chatId, String productInfo) {
        telegramBot.execute(new SendMessage(chatId,
                "✅ <b>9/9</b> Mahsulot ma'lumotlari:\n\n" + productInfo +
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

    @Override
    public void sendUsersStatistics(Long chatId, long totalCount, long adminCount, long superAdminCount, boolean isSuperAdmin) {
        try {
            StringBuilder message = new StringBuilder();
            message.append("👥 <b>Foydalanuvchilar statistikasi</b>\n\n");
            message.append("📊 Umumiy foydalanuvchilar: ").append(totalCount).append(" ta\n");
            message.append("👨‍💼 Adminlar: ").append(adminCount).append(" ta\n");
            message.append("👑 Super Adminlar: ").append(superAdminCount).append(" ta\n");
            
            if (isSuperAdmin) {
                message.append("\n⬇️ Quyidagi tugmani bosib, admin qo'shishingiz mumkin:");
                telegramBot.execute(new SendMessage(chatId, message.toString())
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createSetAdminButton()));
            } else {
                telegramBot.execute(new SendMessage(chatId, message.toString())
                        .parseMode(ParseMode.HTML));
            }
            SendMessage cancelMessage = new SendMessage(chatId, " ")
                    .replyMarkup(buttonService.createAdminCancelReplyKeyboard());
            telegramBot.execute(cancelMessage);
        } catch (Exception e) {
            log.error("❌ Exception while sending users statistics to chatId: {}", chatId, e);
        }
    }

    @Override
    public void sendPhoneNumberPrompt(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📱 Foydalanuvchi telefon raqamini kiriting (masalan: +998901234567):")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendUserNotFound(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "❌ Foydalanuvchi topilmadi!")
                .parseMode(ParseMode.HTML)
        );
        sendAdminMenuWithCancel(chatId);
    }

    @Override
    public void sendUserInfo(Long chatId, User user, boolean canSetAdmin, boolean canSetSuperAdmin) {
        try {
            StringBuilder message = new StringBuilder();
            message.append("👤 <b>Foydalanuvchi ma'lumotlari</b>\n\n");
            message.append("🆔 ID: ").append(user.getId()).append("\n");
            message.append("👤 Ism: ").append(user.getFirstName() != null ? user.getFirstName() : "N/A");
            if (user.getLastName() != null) {
                message.append(" ").append(user.getLastName());
            }
            message.append("\n");
            message.append("📱 Telefon: ").append(user.getPhone() != null ? user.getPhone() : "N/A").append("\n");
            message.append("🔖 Username: ").append(user.getTgUsername() != null ? "@" + user.getTgUsername() : "N/A").append("\n");
            
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                message.append("👥 Rollar: ");
                for (int i = 0; i < user.getRoles().size(); i++) {
                    Role role = user.getRoles().get(i);
                    if (i > 0) message.append(", ");
                    message.append(role.getName() != null ? role.getName() : "N/A");
                }
                message.append("\n");
            }
            
            telegramBot.execute(new SendMessage(chatId, message.toString())
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createUserRoleButtons(canSetAdmin, canSetSuperAdmin, user.getId())));
        } catch (Exception e) {
            log.error("❌ Exception while sending user info to chatId: {}", chatId, e);
        }
    }

    @Override
    public void sendRoleAddedSuccess(Long chatId, String roleName) {
        telegramBot.execute(new SendMessage(chatId,
                "✅ " + roleName + " role muvaffaqiyatli qo'shildi!")
                .parseMode(ParseMode.HTML)
        );
    }
}
