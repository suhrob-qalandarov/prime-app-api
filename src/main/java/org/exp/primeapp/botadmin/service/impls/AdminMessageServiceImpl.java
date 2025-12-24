package org.exp.primeapp.botadmin.service.impls;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.botadmin.service.interfaces.AdminButtonService;
import org.exp.primeapp.botadmin.service.interfaces.AdminMessageService;
import org.exp.primeapp.models.entities.Attachment;
import org.exp.primeapp.models.entities.Role;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.repository.AttachmentRepository;
import org.exp.primeapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AdminMessageServiceImpl implements AdminMessageService {

    private final TelegramBot telegramBot;
    private final AdminButtonService buttonService;
    private final org.exp.primeapp.botuser.service.impls.UserServiceImpl botUserService;
    private final UserRepository userRepository;
    private final TelegramBot userBot;
    private final AttachmentRepository attachmentRepository;

    public AdminMessageServiceImpl(@Qualifier("adminBot") TelegramBot telegramBot,
                                   AdminButtonService buttonService,
                                   org.exp.primeapp.botuser.service.impls.UserServiceImpl botUserService,
                                   UserRepository userRepository,
                                   @Qualifier("userBot") TelegramBot userBot,
                                   AttachmentRepository attachmentRepository) {
        this.telegramBot = telegramBot;
        this.buttonService = buttonService;
        this.botUserService = botUserService;
        this.userRepository = userRepository;
        this.userBot = userBot;
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    public void sendAccessDeniedMessage(Long chatId, String userBotUsername) {
        try {
            if (telegramBot == null) {
                log.error("❌ Telegram bot is null! Cannot send access denied message to chatId: {}", chatId);
                return;
            }
            
            String message = "❌ Kirish mumkin emas!\n\n" +
                    "Bu bot faqat adminlar uchun.\n" +
                    "Foydalanuvchilar uchun bot: @" + userBotUsername;
            
            telegramBot.execute(new SendMessage(chatId, message));
        } catch (Exception e) {
            log.error("Error sending access denied message to chatId: {}", chatId, e);
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
                // Send message with reply keyboard
                SendMessage productMessage = new SendMessage(chatId,
                        "🛍️ <b>Mahsulotlar bo'limi</b>\n\nQuyidagi amallardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createProductReplyKeyboard());
                telegramBot.execute(productMessage);
                return;
            } else if (sectionName.equals("Kategoriyalar")) {
                // First, remove old keyboard and set new cancel button
                SendMessage cancelMessage = new SendMessage(chatId,
                        "📂 <b>Kategoriyalar bo'limi</b>\n\nQuyidagi amallardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createAdminCancelReplyKeyboard());
                telegramBot.execute(cancelMessage);
                
                // Then send inline keyboard message
                SendMessage inlineMessage = new SendMessage(chatId,
                        "⬇️ Quyidagi tugmalardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createCategoryMenuButtons());
                telegramBot.execute(inlineMessage);
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
    public void sendProductCreationStart(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "🛍️ <b>Yangi mahsulot qo'shish</b>\n\n" +
                "Mahsulot qo'shish jarayonini boshlaymiz. Quyidagi ma'lumotlarni ketma-ket kiriting:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createProductCreationCancelReplyKeyboard())
        );
    }

    @Override
    public Integer sendProductNamePrompt(Long chatId) {
        SendResponse response = telegramBot.execute(new SendMessage(chatId,
                "📝 <b>1/9</b> Mahsulot nomini kiriting:")
                .parseMode(ParseMode.HTML)
        );
        return response.message() != null ? response.message().messageId() : null;
    }

    @Override
    public Integer sendProductDescriptionPrompt(Long chatId) {
        SendResponse response = telegramBot.execute(new SendMessage(chatId,
                "📝 <b>2/9</b> Mahsulot tavsifini kiriting:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createBackButton("WAITING_NAME"))
        );
        return response.message() != null ? response.message().messageId() : null;
    }

    @Override
    public Integer sendProductBrandPrompt(Long chatId) {
        InlineKeyboardMarkup nextStepButton = buttonService.createNextStepButton();
        InlineKeyboardMarkup withBack = buttonService.addBackButton(nextStepButton, "WAITING_DESCRIPTION");
        
        SendResponse response = telegramBot.execute(new SendMessage(chatId,
                "🏷️ <b>3/9</b> Brend nomini kiriting:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(withBack)
        );
        return response.message() != null ? response.message().messageId() : null;
    }

    @Override
    public Integer sendProductColorPrompt(Long chatId) {
        InlineKeyboardMarkup colorButtons = buttonService.createColorButtons();
        InlineKeyboardMarkup withBack = ((AdminButtonServiceImpl) buttonService).addBackButton(colorButtons, "WAITING_BRAND");
        
        SendResponse response = telegramBot.execute(new SendMessage(chatId,
                "🎨 <b>4/9</b> Rangni tanlang:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(withBack)
        );
        return response.message() != null ? response.message().messageId() : null;
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
    public Integer sendMainImagePrompt(Long chatId) {
        SendResponse response = telegramBot.execute(new SendMessage(chatId,
                "📷 <b>5/9</b> Mahsulotning asosiy rasmlarini yuboring:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createBackButton("WAITING_COLOR"))
        );
        return response.message() != null ? response.message().messageId() : null;
    }

    @Override
    public void sendAdditionalImagesPrompt(Long chatId, int currentCount) {
        String message = "📷 <b>5/9</b> Mahsulotning qo'shimcha rasmlarini yuboring:\n\n";
        message += "• Maksimum: 2 ta qo'shimcha rasm\n";
        message += "• Hozirgi: " + currentCount + " ta";
        
        InlineKeyboardMarkup skipButton = buttonService.createSkipAdditionalImagesButton();
        InlineKeyboardMarkup withBack = buttonService.addBackButton(skipButton, "WAITING_MAIN_IMAGE");
        
        telegramBot.execute(new SendMessage(chatId, message)
                .parseMode(ParseMode.HTML)
                .replyMarkup(withBack)
        );
    }

    @Override
    public void sendImageSavedSuccess(Long chatId, int currentCount, int remaining) {
        String message = "✅ Asosiy rasm muvaffaqiyatli saqlandi!\n";
        message += "➕ Qo'shish mumkin yana " + remaining + " ta qo'shimcha rasm!";
        
        // Create buttons: Previous step (back to WAITING_MAIN_IMAGE) and Next step (skip to WAITING_SPOTLIGHT_NAME)
        InlineKeyboardButton previousButton = new InlineKeyboardButton("⬅️ Previous step")
                .callbackData("back_to_WAITING_MAIN_IMAGE");
        InlineKeyboardButton nextButton = new InlineKeyboardButton("➡️ Next step")
                .callbackData("continue_images");
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{previousButton, nextButton}
        );
        
        telegramBot.execute(new SendMessage(chatId, message)
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboard)
        );
    }

    @Override
    public void sendSpotlightNamePromptForProduct(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📂 <b>6/9</b> Toifani tanlang:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createSpotlightNameButtonsWithBack())
        );
    }

    @Override
    public void sendCategorySelection(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📂 <b>7/9</b> Kategoriyani tanlang:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createBackButton("WAITING_SPOTLIGHT_NAME"))
        );
    }

    @Override
    public void sendSizeSelection(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "📏 <b>8/9</b> O'lchamlarni tanlang (bir nechtasini tanlash mumkin):")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createBackButton("WAITING_CATEGORY"))
        );
    }

    @Override
    public Integer sendProductPricePrompt(Long chatId) {
        SendResponse response = telegramBot.execute(new SendMessage(chatId,
                "💰 <b>9/9</b> Mahsulot narxini kiriting (so'm):")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createBackButton("WAITING_QUANTITIES"))
        );
        return response.message() != null ? response.message().messageId() : null;
    }

    @Override
    public void sendProductConfirmation(Long chatId, String productInfo, org.exp.primeapp.botadmin.models.ProductCreationState state) {
        // Prepare caption with product info
        String caption = "✅ <b>9/9</b> Mahsulot ma'lumotlari:\n\n" + productInfo +
                "\n\nMahsulotni qo'shishni tasdiqlaysizmi?";
        
        // Send images as media group if available
        if (state != null && state.getAttachmentUrls() != null && !state.getAttachmentUrls().isEmpty()) {
            List<InputMediaPhoto> mediaList = new ArrayList<>();
            
            for (int i = 0; i < state.getAttachmentUrls().size(); i++) {
                String attachmentUrl = state.getAttachmentUrls().get(i);
                try {
                    Attachment attachment = attachmentRepository.findByUrl(attachmentUrl);
                    if (attachment != null && attachment.getFilePath() != null) {
                        java.io.File photoFile = new java.io.File(attachment.getFilePath());
                        if (photoFile.exists() && photoFile.isFile()) {
                            // First photo gets the caption, others don't
                            if (i == 0) {
                                mediaList.add(new InputMediaPhoto(photoFile).caption(caption).parseMode(ParseMode.HTML));
                            } else {
                                mediaList.add(new InputMediaPhoto(photoFile));
                            }
                        } else {
                            log.warn("Image file not found: {}", attachment.getFilePath());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error preparing product image: {}", e.getMessage(), e);
                }
            }
            
            // Send media group if we have images
            if (!mediaList.isEmpty()) {
                // Telegram allows max 10 media in a group
                if (mediaList.size() <= 10) {
                    telegramBot.execute(new SendMediaGroup(chatId, mediaList.toArray(new InputMediaPhoto[0])));
                } else {
                    // If more than 10 images, send first 10 as group with caption
                    telegramBot.execute(new SendMediaGroup(chatId, mediaList.subList(0, 10).toArray(new InputMediaPhoto[0])));
                    // Send remaining images as separate group (without caption)
                    List<InputMediaPhoto> remainingMedia = new ArrayList<>();
                    for (int i = 10; i < mediaList.size(); i++) {
                        remainingMedia.add(mediaList.get(i));
                    }
                    if (!remainingMedia.isEmpty()) {
                        telegramBot.execute(new SendMediaGroup(chatId, remainingMedia.toArray(new InputMediaPhoto[0])));
                    }
                }
                
                // Send confirmation buttons as separate message
                telegramBot.execute(new SendMessage(chatId, "Mahsulotni qo'shishni tasdiqlaysizmi?")
                        .replyMarkup(buttonService.createProductConfirmationReplyKeyboard()));
            } else {
                // No images, send text message with buttons
                telegramBot.execute(new SendMessage(chatId, caption)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createProductConfirmationReplyKeyboard()));
            }
        } else {
            // No images, send text message with buttons
            telegramBot.execute(new SendMessage(chatId, caption)
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createProductConfirmationReplyKeyboard()));
        }
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
    public void sendProductSizeQuantityPrompt(Long chatId, org.exp.primeapp.botadmin.models.ProductCreationState state) {
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
                .replyMarkup(buttonService.createBackButton("WAITING_SIZES"))
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
            message.append("👥 <b>Foydalanuvchilar bo'limi</b>\n\n");
            message.append("📊 Umumiy foydalanuvchilar: ").append(totalCount).append(" ta\n");
            message.append("👨‍💼 Adminlar: ").append(adminCount).append(" ta\n");
            message.append("👑 Super Adminlar: ").append(superAdminCount).append(" ta\n");
            
            // Send main message with cancel button
            SendMessage mainMessage = new SendMessage(chatId, message.toString())
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createAdminCancelReplyKeyboard());
            telegramBot.execute(mainMessage);
            
            // Send inline button if super admin
            if (isSuperAdmin) {
                telegramBot.execute(new SendMessage(chatId,
                        "⬇️ Quyidagi tugmani bosib, admin qo'shishingiz mumkin:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createSetAdminButton()));
            }
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

    @Override
    public void sendNoCategoriesMessage(Long chatId) {
        telegramBot.execute(new SendMessage(chatId,
                "⚠️ <b>Kategoriya mavjud emas!</b>\n\n" +
                "Birorta ham kategoriya mavjud emas. Avval kategoriya qo'shing!")
                .parseMode(ParseMode.HTML)
        );
    }

    @Override
    public void sendSimpleMessage(Long chatId, String message) {
        telegramBot.execute(new SendMessage(chatId, message)
                .parseMode(ParseMode.HTML)
        );
    }
}
