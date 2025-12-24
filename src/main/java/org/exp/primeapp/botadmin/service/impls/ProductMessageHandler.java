package org.exp.primeapp.botadmin.service.impls;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botadmin.models.ProductCreationState;
import org.exp.primeapp.botadmin.service.interfaces.AdminMessageService;
import org.exp.primeapp.botadmin.service.interfaces.BotProductService;
import org.exp.primeapp.botadmin.utils.AdminBotMsgConst;
import org.exp.primeapp.models.entities.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductMessageHandler {
    
    private final BotProductService botProductService;
    private final AdminMessageService messageService;
    private final TelegramBot telegramBot;
    
    public ProductMessageHandler(BotProductService botProductService,
                                 AdminMessageService messageService,
                                 @Qualifier("adminBot") TelegramBot telegramBot) {
        this.botProductService = botProductService;
        this.messageService = messageService;
        this.telegramBot = telegramBot;
    }
    
    public boolean handleMessage(Message message, User user) {
        String text = message.text();
        Long userId = user.getId();
        Long chatId = user.getTelegramId();
        
        // Check if user is in product creation flow
        ProductCreationState state = botProductService.getProductCreationState(userId);
        if (state == null) {
            return false;
        }
        
        // Handle photo messages
        if (message.photo() != null && message.photo().length > 0) {
            handleProductImage(message, user, state);
            return true;
        }
        
        // Handle text messages
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Handle product creation buttons
        if (text.equals(AdminBotMsgConst.BTN_NEW_PRODUCT)) {
            botProductService.startProductCreation(userId);
            messageService.sendProductCreationStart(chatId);
            Integer nameMessageId = messageService.sendProductNamePrompt(chatId);
            if (nameMessageId != null) {
                state.setStepMessageId(ProductCreationState.Step.WAITING_NAME, nameMessageId);
            }
            return true;
        }
        
        if (text.equals(AdminBotMsgConst.BTN_EDIT_PRODUCT)) {
            messageService.sendSimpleMessage(chatId, "⚠️ <b>Mahsulot o'zgartirish</b> funksiyasi keyinroq qo'shiladi");
            return true;
        }
        
        if (text.equals(AdminBotMsgConst.BTN_INCOME)) {
            messageService.sendSimpleMessage(chatId, "⚠️ <b>Income</b> funksiyasi keyinroq qo'shiladi");
            return true;
        }
        
        if (text.equals(AdminBotMsgConst.BTN_OUTCOME)) {
            messageService.sendSimpleMessage(chatId, "⚠️ <b>Outcome</b> funksiyasi keyinroq qo'shiladi");
            return true;
        }
        
        if (text.equals(AdminBotMsgConst.BTN_CANCEL_PRODUCT)) {
            botProductService.cancelProductCreation(userId);
            messageService.sendProductCreationCancelled(chatId);
            messageService.sendAdminSectionMessage(chatId, "Mahsulotlar");
            return true;
        }
        
        // Handle product creation steps
        handleProductCreationMessage(message, user, state);
        return true;
    }
    
    private void handleProductImage(Message message, User user, ProductCreationState state) {
        Long userId = user.getId();
        Long chatId = user.getTelegramId();
        
        if (state.getCurrentStep() == ProductCreationState.Step.WAITING_MAIN_IMAGE || 
            state.getCurrentStep() == ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES) {
            // Get the largest photo
            PhotoSize[] photos = message.photo();
            PhotoSize largestPhoto = photos[photos.length - 1];
            String fileId = largestPhoto.fileId();
            
            botProductService.handleProductImage(userId, fileId);
            
            int currentCount = state.getAttachmentUrls() != null ? state.getAttachmentUrls().size() : 0;
            
            if (state.getCurrentStep() == ProductCreationState.Step.WAITING_MAIN_IMAGE) {
                // Main image uploaded, move to additional images step
                messageService.sendAdditionalImagesPrompt(chatId, currentCount);
            } else if (state.getCurrentStep() == ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES) {
                // Additional image uploaded
                int remaining = 3 - currentCount;
                if (currentCount >= 3) {
                    // Max 3 images reached
                    messageService.sendImagesCompleted(chatId, currentCount);
                    state.setCurrentStep(ProductCreationState.Step.WAITING_SPOTLIGHT_NAME);
                    messageService.sendSpotlightNamePromptForProduct(chatId);
                } else {
                    // Can add more additional images
                    messageService.sendImageSavedSuccess(chatId, currentCount, remaining);
                }
            }
        }
    }
    
    private void handleProductCreationMessage(Message message, User user, ProductCreationState state) {
        String text = message.text();
        Long chatId = user.getTelegramId();
        Long userId = user.getId();
        
        switch (state.getCurrentStep()) {
            case WAITING_NAME:
                // Delete current step message
                Integer currentNameMessageId = state.getStepMessageId(ProductCreationState.Step.WAITING_NAME);
                if (currentNameMessageId != null) {
                    try {
                        telegramBot.execute(new DeleteMessage(chatId, currentNameMessageId));
                    } catch (Exception e) {
                        log.error("Error deleting WAITING_NAME message: {}", e.getMessage());
                    }
                }
                
                botProductService.handleProductName(userId, text);
                Integer descMessageId = messageService.sendProductDescriptionPrompt(chatId);
                if (descMessageId != null) {
                    state.setStepMessageId(ProductCreationState.Step.WAITING_DESCRIPTION, descMessageId);
                }
                break;
                
            case WAITING_DESCRIPTION:
                // Delete current step message
                Integer currentDescMessageId = state.getStepMessageId(ProductCreationState.Step.WAITING_DESCRIPTION);
                if (currentDescMessageId != null) {
                    try {
                        telegramBot.execute(new DeleteMessage(chatId, currentDescMessageId));
                    } catch (Exception e) {
                        log.error("Error deleting WAITING_DESCRIPTION message: {}", e.getMessage());
                    }
                }
                
                // Resend previous step (WAITING_NAME) message without inline buttons
                resendPreviousStepMessage(chatId, state, ProductCreationState.Step.WAITING_DESCRIPTION);
                
                botProductService.handleProductDescription(userId, text);
                Integer brandMessageId = messageService.sendProductBrandPrompt(chatId);
                if (brandMessageId != null) {
                    state.setStepMessageId(ProductCreationState.Step.WAITING_BRAND, brandMessageId);
                }
                break;
                
            case WAITING_BRAND:
                // Delete current step message
                Integer currentBrandMessageId = state.getStepMessageId(ProductCreationState.Step.WAITING_BRAND);
                if (currentBrandMessageId != null) {
                    try {
                        telegramBot.execute(new DeleteMessage(chatId, currentBrandMessageId));
                    } catch (Exception e) {
                        log.error("Error deleting WAITING_BRAND message: {}", e.getMessage());
                    }
                }
                
                // Resend previous step (WAITING_DESCRIPTION) message without inline buttons
                resendPreviousStepMessage(chatId, state, ProductCreationState.Step.WAITING_BRAND);
                
                // Brand is optional - if empty, skip to next step
                if (text != null && !text.trim().isEmpty()) {
                    botProductService.handleProductBrand(userId, text);
                } else {
                    botProductService.handleProductBrand(userId, "");
                }
                // After brand, move to color selection step
                state.setCurrentStep(ProductCreationState.Step.WAITING_COLOR);
                messageService.sendProductColorPrompt(chatId);
                break;
                
            case WAITING_QUANTITIES:
                // Delete current step message and resend previous step message (only on first quantity input)
                // Check if this is the first quantity being entered
                boolean isFirstQuantity = state.getSizeQuantities().values().stream()
                        .allMatch(qty -> qty == null || qty == 0);
                if (isFirstQuantity) {
                    // Delete current step message if exists
                    Integer currentQuantitiesMessageId = state.getStepMessageId(ProductCreationState.Step.WAITING_QUANTITIES);
                    if (currentQuantitiesMessageId != null) {
                        try {
                            telegramBot.execute(new DeleteMessage(chatId, currentQuantitiesMessageId));
                        } catch (Exception e) {
                            log.error("Error deleting WAITING_QUANTITIES message: {}", e.getMessage());
                        }
                    }
                    // Resend previous step message without inline buttons
                    resendPreviousStepMessage(chatId, state, ProductCreationState.Step.WAITING_QUANTITIES);
                }
                
                // Handle quantity input for sizes
                try {
                    Integer quantity = Integer.parseInt(text.trim());
                    if (quantity <= 0) {
                        messageService.sendProductSizeQuantityPrompt(user.getTelegramId(), state);
                        return;
                    }
                    
                    // Find the first size without quantity
                    org.exp.primeapp.models.enums.Size sizeToSet = null;
                    for (org.exp.primeapp.models.enums.Size size : state.getSelectedSizes()) {
                        if (!state.getSizeQuantities().containsKey(size) || 
                            state.getSizeQuantities().get(size) == null || 
                            state.getSizeQuantities().get(size) == 0) {
                            sizeToSet = size;
                            break;
                        }
                    }
                    
                    if (sizeToSet != null) {
                        botProductService.handleSizeQuantity(userId, sizeToSet.name(), quantity);
                        
                        // Check if all sizes have quantities
                        boolean allSizesHaveQuantities = true;
                        for (org.exp.primeapp.models.enums.Size size : state.getSelectedSizes()) {
                            if (!state.getSizeQuantities().containsKey(size) || 
                                state.getSizeQuantities().get(size) == null || 
                                state.getSizeQuantities().get(size) == 0) {
                                allSizesHaveQuantities = false;
                                break;
                            }
                        }
                        
                        if (allSizesHaveQuantities) {
                            // All quantities set, ask for price
                            state.setCurrentStep(ProductCreationState.Step.WAITING_PRICE);
                            messageService.sendProductPricePrompt(user.getTelegramId());
                        } else {
                            // Ask for next size quantity
                            messageService.sendProductSizeQuantityPrompt(user.getTelegramId(), state);
                        }
                    } else {
                        // All quantities set, ask for price
                        state.setCurrentStep(ProductCreationState.Step.WAITING_PRICE);
                        messageService.sendProductPricePrompt(user.getTelegramId());
                    }
                } catch (NumberFormatException e) {
                    messageService.sendProductSizeQuantityPrompt(user.getTelegramId(), state);
                }
                break;
                
            case WAITING_PRICE:
                // Delete current step message
                Integer currentPriceMessageId = state.getStepMessageId(ProductCreationState.Step.WAITING_PRICE);
                if (currentPriceMessageId != null) {
                    try {
                        telegramBot.execute(new DeleteMessage(chatId, currentPriceMessageId));
                    } catch (Exception e) {
                        log.error("Error deleting WAITING_PRICE message: {}", e.getMessage());
                    }
                }
                
                // Resend previous step message without inline buttons
                resendPreviousStepMessage(chatId, state, ProductCreationState.Step.WAITING_PRICE);
                
                try {
                    botProductService.handleProductPrice(userId, text);
                    String productInfo = buildProductInfo(state);
                    messageService.sendProductConfirmation(chatId, productInfo);
                } catch (RuntimeException e) {
                    // Invalid price format
                    messageService.sendProductPricePrompt(user.getTelegramId());
                }
                break;
                
            default:
                break;
        }
    }
    
    private String buildProductInfo(ProductCreationState state) {
        StringBuilder info = new StringBuilder();
        info.append("<b>Nomi:</b> ").append(state.getName()).append("\n");
        info.append("<b>Brend:</b> ").append(state.getBrand()).append("\n");
        info.append("<b>Tavsif:</b> ").append(state.getDescription()).append("\n");
        if (state.getCategory() != null) {
            info.append("<b>Kategoriya:</b> ").append(state.getCategory().getName()).append("\n");
        }
        if (state.getPrice() != null) {
            info.append("<b>Narx:</b> ").append(state.getPrice()).append(" so'm\n");
        }
        info.append("<b>Rasmlar:</b> ").append(state.getAttachmentUrls() != null ? state.getAttachmentUrls().size() : 0).append(" ta\n");
        if (state.getSelectedSizes() != null && !state.getSelectedSizes().isEmpty()) {
            info.append("<b>O'lchamlar:</b>\n");
            for (org.exp.primeapp.models.enums.Size size : state.getSelectedSizes()) {
                Integer qty = state.getSizeQuantities().getOrDefault(size, 0);
                info.append("  • ").append(size.getLabel()).append(": ").append(qty).append(" ta\n");
            }
        }
        return info.toString();
    }
    
    private void resendPreviousStepMessage(Long chatId, ProductCreationState state, ProductCreationState.Step currentStep) {
        // Get previous step
        ProductCreationState.Step previousStep = getPreviousStep(currentStep);
        if (previousStep == null) {
            return;
        }
        
        // Get previous step message ID and delete it
        Integer previousMessageId = state.getStepMessageId(previousStep);
        if (previousMessageId != null) {
            try {
                telegramBot.execute(new DeleteMessage(chatId, previousMessageId));
            } catch (Exception e) {
                log.error("Error deleting previous step message: {}", e.getMessage());
            }
        }
        
        // Get message text for previous step
        String previousStepText = getStepMessageText(previousStep);
        if (previousStepText == null) {
            return;
        }
        
        // Resend previous step message without inline buttons
        try {
            SendResponse response = telegramBot.execute(new SendMessage(chatId, previousStepText)
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
            );
            
            // Save new message ID
            if (response.message() != null && response.message().messageId() != null) {
                state.setStepMessageId(previousStep, response.message().messageId());
            }
        } catch (Exception e) {
            log.error("Error resending previous step message: {}", e.getMessage());
        }
    }
    
    private ProductCreationState.Step getPreviousStep(ProductCreationState.Step currentStep) {
        return switch (currentStep) {
            case WAITING_NAME -> null;
            case WAITING_DESCRIPTION -> ProductCreationState.Step.WAITING_NAME;
            case WAITING_BRAND -> ProductCreationState.Step.WAITING_DESCRIPTION;
            case WAITING_COLOR -> ProductCreationState.Step.WAITING_BRAND;
            case WAITING_MAIN_IMAGE -> ProductCreationState.Step.WAITING_COLOR;
            case WAITING_ADDITIONAL_IMAGES -> ProductCreationState.Step.WAITING_MAIN_IMAGE;
            case WAITING_SPOTLIGHT_NAME -> ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES;
            case WAITING_CATEGORY -> ProductCreationState.Step.WAITING_SPOTLIGHT_NAME;
            case WAITING_SIZES -> ProductCreationState.Step.WAITING_CATEGORY;
            case WAITING_QUANTITIES -> ProductCreationState.Step.WAITING_SIZES;
            case WAITING_PRICE -> ProductCreationState.Step.WAITING_QUANTITIES;
            default -> null;
        };
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

