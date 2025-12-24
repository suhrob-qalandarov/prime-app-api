package org.exp.primeapp.botadmin.service.impls;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botadmin.models.ProductCreationState;
import org.exp.primeapp.botadmin.service.interfaces.AdminButtonService;
import org.exp.primeapp.botadmin.service.interfaces.AdminMessageService;
import org.exp.primeapp.botadmin.service.interfaces.BotProductService;
import org.exp.primeapp.botadmin.utils.AdminBotMsgConst;
import org.exp.primeapp.models.enums.Size;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProductCallbackHandler {
    
    private final BotProductService botProductService;
    private final AdminMessageService messageService;
    private final AdminButtonService buttonService;
    private final TelegramBot telegramBot;
    
    public ProductCallbackHandler(BotProductService botProductService,
                                  AdminMessageService messageService,
                                  AdminButtonService buttonService,
                                  @Qualifier("adminBot") TelegramBot telegramBot) {
        this.botProductService = botProductService;
        this.messageService = messageService;
        this.buttonService = buttonService;
        this.telegramBot = telegramBot;
    }
    
    public boolean handleCallback(CallbackQuery callbackQuery, Long userId, Long chatId) {
        String data = callbackQuery.data();
        String callbackId = callbackQuery.id();
        
        // Check if this is a product-related callback
        if (!isProductCallback(data)) {
            return false;
        }
        
        // Get product creation state
        ProductCreationState state = botProductService.getProductCreationState(userId);
        
        // Handle add_product callback (can be called without active state)
        if (data.equals(AdminBotMsgConst.CALLBACK_ADD_PRODUCT)) {
            botProductService.startProductCreation(userId);
            messageService.sendProductCreationStart(chatId);
            messageService.sendProductNamePrompt(chatId);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Mahsulot qo'shish boshlandi"));
            return true;
        }
        
        // For other callbacks, state must exist
        if (state == null) {
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Mahsulot qo'shish jarayoni topilmadi. /add_product bilan boshlang.")
                    .showAlert(true));
            return true;
        }
        
        // Handle back button callbacks
        if (data.startsWith(AdminBotMsgConst.CALLBACK_BACK_TO)) {
            handleBackButton(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle skip brand callback
        if (data.equals(AdminBotMsgConst.CALLBACK_SKIP_BRAND)) {
            handleSkipBrand(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle color selection callbacks
        if (data.startsWith("select_color_")) {
            handleColorSelection(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle skip color callback
        if (data.equals(AdminBotMsgConst.CALLBACK_SKIP_COLOR)) {
            handleSkipColor(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle category selection
        if (data.startsWith("select_category_")) {
            handleCategorySelection(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle size selection
        if (data.startsWith("toggle_size_")) {
            handleSizeSelection(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle continue sizes
        if (data.equals("continue_sizes")) {
            handleContinueSizes(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle continue images
        if (data.equals("continue_images")) {
            handleContinueImages(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle confirm product
        if (data.equals(AdminBotMsgConst.CALLBACK_CONFIRM_PRODUCT)) {
            handleConfirmProduct(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        // Handle cancel product
        if (data.equals(AdminBotMsgConst.CALLBACK_CANCEL_PRODUCT)) {
            handleCancelProduct(callbackQuery, userId, chatId, state, callbackId);
            return true;
        }
        
        return false;
    }
    
    private boolean isProductCallback(String data) {
        return data.equals(AdminBotMsgConst.CALLBACK_ADD_PRODUCT) ||
               data.equals(AdminBotMsgConst.CALLBACK_SKIP_BRAND) ||
               data.equals(AdminBotMsgConst.CALLBACK_SKIP_COLOR) ||
               data.startsWith(AdminBotMsgConst.CALLBACK_BACK_TO) ||
               data.startsWith("select_color_") ||
               data.startsWith("select_category_") ||
               data.startsWith("toggle_size_") ||
               data.equals("continue_sizes") ||
               data.equals("continue_images") ||
               data.equals(AdminBotMsgConst.CALLBACK_CONFIRM_PRODUCT) ||
               data.equals(AdminBotMsgConst.CALLBACK_CANCEL_PRODUCT);
    }
    
    private void handleBackButton(CallbackQuery callbackQuery, Long userId, Long chatId, 
                                  ProductCreationState state, String callbackId) {
        String data = callbackQuery.data();
        String stepName = data.replace(AdminBotMsgConst.CALLBACK_BACK_TO, "");
        ProductCreationState.Step targetStep = null;
        
        try {
            targetStep = ProductCreationState.Step.valueOf(stepName);
        } catch (IllegalArgumentException e) {
            log.error("Invalid step name: {}", stepName);
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Xatolik: Noto'g'ri qadam")
                    .showAlert(true));
            return;
        }
        
        // Delete current step message
        com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
        Integer currentMessageId = callbackMessage != null ? callbackMessage.messageId() : null;
        if (currentMessageId != null) {
            try {
                telegramBot.execute(new DeleteMessage(chatId, currentMessageId));
            } catch (Exception e) {
                log.error("Error deleting current step message: {}", e.getMessage());
            }
        }
        
        // Delete previous step message if exists
        Integer previousStepMessageId = state.getStepMessageId(targetStep);
        if (previousStepMessageId != null) {
            try {
                telegramBot.execute(new DeleteMessage(chatId, previousStepMessageId));
            } catch (Exception e) {
                log.error("Error deleting previous step message: {}", e.getMessage());
            }
        }
        
        // Set the state to target step directly
        state.setCurrentStep(targetStep);
        
        // Resend previous step message with appropriate inline buttons
        Integer newMessageId = null;
        switch (targetStep) {
            case WAITING_NAME:
                newMessageId = messageService.sendProductNamePrompt(chatId);
                break;
            case WAITING_DESCRIPTION:
                newMessageId = messageService.sendProductDescriptionPrompt(chatId);
                break;
            case WAITING_BRAND:
                newMessageId = messageService.sendProductBrandPrompt(chatId);
                break;
            case WAITING_COLOR:
                messageService.sendProductColorPrompt(chatId);
                break;
            default:
                log.warn("No handler for back to step: {}", targetStep);
                break;
        }
        
        // Save new message ID to state
        if (newMessageId != null) {
            state.setStepMessageId(targetStep, newMessageId);
        }
        
        telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Orqaga qaytildi"));
    }
    
    private void handleSkipBrand(CallbackQuery callbackQuery, Long userId, Long chatId,
                                 ProductCreationState state, String callbackId) {
        botProductService.handleProductBrand(userId, "");
        
        com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
        Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
        if (messageId != null) {
            telegramBot.execute(new EditMessageText(chatId, messageId,
                    "🏷️ <b>3/9</b> Brend nomi: (O'tkazib yuborildi)")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
            );
        }
        
        state.setCurrentStep(ProductCreationState.Step.WAITING_COLOR);
        messageService.sendProductColorPrompt(chatId);
        telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Brend o'tkazib yuborildi"));
    }
    
    private void handleColorSelection(CallbackQuery callbackQuery, Long userId, Long chatId,
                                     ProductCreationState state, String callbackId) {
        String data = callbackQuery.data();
        String colorData = data.replace("select_color_", "");
        int lastUnderscoreIndex = colorData.lastIndexOf("_");
        if (lastUnderscoreIndex > 0 && lastUnderscoreIndex < colorData.length() - 1) {
            String colorName = colorData.substring(0, lastUnderscoreIndex);
            String colorHex = colorData.substring(lastUnderscoreIndex + 1);
            
            botProductService.handleProductColor(userId, colorName, colorHex);
            
            com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
            Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
            if (messageId != null) {
                telegramBot.execute(new EditMessageText(chatId, messageId,
                        "🎨 <b>4/9</b> Rang tanlandi: " + colorName)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
                );
            }
            
            state.setCurrentStep(ProductCreationState.Step.WAITING_MAIN_IMAGE);
            messageService.sendMainImagePrompt(chatId);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Rang tanlandi"));
        } else {
            log.error("Failed to parse color data: {}", colorData);
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Xatolik: Rang ma'lumotlarini parse qilishda muammo")
                    .showAlert(true));
        }
    }
    
    private void handleSkipColor(CallbackQuery callbackQuery, Long userId, Long chatId,
                                 ProductCreationState state, String callbackId) {
        botProductService.handleProductColor(userId, "N/A", "#000000");
        
        com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
        Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
        if (messageId != null) {
            telegramBot.execute(new EditMessageText(chatId, messageId,
                    "🎨 <b>4/9</b> Rang tanlash: (O'tkazib yuborildi)")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
            );
        }
        
        state.setCurrentStep(ProductCreationState.Step.WAITING_MAIN_IMAGE);
        messageService.sendMainImagePrompt(chatId);
        telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Rang tanlash o'tkazib yuborildi"));
    }
    
    private void handleCategorySelection(CallbackQuery callbackQuery, Long userId, Long chatId,
                                        ProductCreationState state, String callbackId) {
        String data = callbackQuery.data();
        Long categoryId = Long.parseLong(data.replace("select_category_", ""));
        botProductService.handleCategorySelection(userId, categoryId);
        
        state.setCurrentStep(ProductCreationState.Step.WAITING_SIZES);
        messageService.sendSizeSelection(chatId);
        List<Size> allSizes = List.of(Size.values());
        telegramBot.execute(new SendMessage(chatId, "📏 O'lchamlarni tanlang (bir nechtasini tanlash mumkin):")
                .parseMode(ParseMode.HTML)
                .replyMarkup(buttonService.createSizeButtons(allSizes, state.getSelectedSizes()))
        );
        
        telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Kategoriya tanlandi"));
    }
    
    private void handleSizeSelection(CallbackQuery callbackQuery, Long userId, Long chatId,
                                     ProductCreationState state, String callbackId) {
        String data = callbackQuery.data();
        String sizeName = data.replace("toggle_size_", "");
        botProductService.handleSizeSelection(userId, sizeName);
        
        List<Size> allSizes = List.of(Size.values());
        com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
        Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
        if (messageId != null) {
            telegramBot.execute(new EditMessageText(chatId, messageId,
                    "📏 O'lchamlarni tanlang:")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createSizeButtons(allSizes, state.getSelectedSizes()))
            );
        }
        
        telegramBot.execute(new AnswerCallbackQuery(callbackId).text("O'lcham tanlandi"));
    }
    
    private void handleContinueSizes(CallbackQuery callbackQuery, Long userId, Long chatId,
                                     ProductCreationState state, String callbackId) {
        if (state.getSelectedSizes().isEmpty()) {
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Kamida bitta o'lcham tanlang")
                    .showAlert(true));
            return;
        }
        
        state.setCurrentStep(ProductCreationState.Step.WAITING_QUANTITIES);
        StringBuilder quantityPrompt = new StringBuilder("📊 Har bir o'lcham uchun miqdorni kiriting:\n\n");
        for (Size size : state.getSelectedSizes()) {
            quantityPrompt.append(size.getLabel()).append(": /qty_").append(size.name()).append("\n");
        }
        
        telegramBot.execute(new SendMessage(chatId, quantityPrompt.toString())
                .parseMode(ParseMode.HTML)
        );
        
        telegramBot.execute(new AnswerCallbackQuery(callbackId).text("O'lchamlar tanlandi"));
    }
    
    private void handleContinueImages(CallbackQuery callbackQuery, Long userId, Long chatId,
                                     ProductCreationState state, String callbackId) {
        if (!state.hasMinimumImages()) {
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Kamida 1 ta rasm yuklashingiz kerak")
                    .showAlert(true));
            return;
        }
        
        int totalCount = state.getAttachmentUrls() != null ? state.getAttachmentUrls().size() : 0;
        messageService.sendImagesCompleted(chatId, totalCount);
        
        state.setCurrentStep(ProductCreationState.Step.WAITING_SPOTLIGHT_NAME);
        messageService.sendSpotlightNamePromptForProduct(chatId);
        
        telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Keyingi qadamga o'tildi"));
    }
    
    private void handleConfirmProduct(CallbackQuery callbackQuery, Long userId, Long chatId,
                                     ProductCreationState state, String callbackId) {
        try {
            botProductService.confirmAndSaveProduct(userId);
            messageService.sendProductSavedSuccess(chatId);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Mahsulot qo'shildi"));
        } catch (Exception e) {
            log.error("Error confirming product: {}", e.getMessage(), e);
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Xatolik: " + e.getMessage())
                    .showAlert(true));
        }
    }
    
    private void handleCancelProduct(CallbackQuery callbackQuery, Long userId, Long chatId,
                                    ProductCreationState state, String callbackId) {
        botProductService.cancelProductCreation(userId);
        messageService.sendProductCreationCancelled(chatId);
        telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Bekor qilindi"));
    }
    
    private String getStepMessageText(ProductCreationState.Step step) {
        return switch (step) {
            case WAITING_NAME -> "📝 <b>1/9</b> Mahsulot nomini kiriting:";
            case WAITING_DESCRIPTION -> "📝 <b>2/9</b> Mahsulot tavsifini kiriting:";
            case WAITING_BRAND -> "🏷️ <b>3/9</b> Brend nomini kiriting:";
            case WAITING_COLOR -> "🎨 <b>4/9</b> Rangni tanlang:";
            case WAITING_MAIN_IMAGE -> "📷 <b>5/9</b> Mahsulotning asosiy rasmlarini yuboring:";
            case WAITING_ADDITIONAL_IMAGES -> "📷 <b>5/9</b> Mahsulotning qo'shimcha rasmlarini yuboring:";
            case WAITING_SPOTLIGHT_NAME -> "📂 <b>6/9</b> Toifani tanlang:";
            case WAITING_CATEGORY -> "📂 <b>7/9</b> Kategoriyani tanlang:";
            case WAITING_SIZES -> "📏 <b>8/9</b> O'lchamlarni tanlang (bir nechtasini tanlash mumkin):";
            case WAITING_QUANTITIES -> "📊 Har bir o'lcham uchun miqdorni kiriting:";
            case WAITING_PRICE -> "💰 <b>9/9</b> Mahsulot narxini kiriting (so'm):";
            default -> null;
        };
    }
}

