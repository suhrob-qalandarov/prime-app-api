package org.exp.primeapp.botadmin.service.impls;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botadmin.models.ProductCreationState;
import org.exp.primeapp.botadmin.service.interfaces.AdminMessageService;
import org.exp.primeapp.botadmin.service.interfaces.BotProductService;
import org.exp.primeapp.botadmin.utils.AdminBotMsgConst;
import org.exp.primeapp.models.entities.User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMessageHandler {
    
    private final BotProductService botProductService;
    private final AdminMessageService messageService;
    
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
            messageService.sendProductNamePrompt(chatId);
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
                botProductService.handleProductName(userId, text);
                messageService.sendProductDescriptionPrompt(chatId);
                break;
                
            case WAITING_DESCRIPTION:
                botProductService.handleProductDescription(userId, text);
                messageService.sendProductBrandPrompt(chatId);
                break;
                
            case WAITING_BRAND:
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
}

