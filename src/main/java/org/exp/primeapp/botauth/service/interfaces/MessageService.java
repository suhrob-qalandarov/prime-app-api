package org.exp.primeapp.botauth.service.interfaces;

import org.exp.primeapp.models.entities.User;

public interface MessageService {

    void sendStartMsg(Long chatId, String firstName);

    void sendStartMsgForAdmin(Long chatId, String firstName);
    
    void sendAdminMenu(Long chatId, String firstName);

    void sendLoginMsg(Long chatId);

    void removeKeyboardAndSendCode(User user);

    void removeKeyboardAndSendMsg(Long telegramId);

    void sendCode(User user);

    void renewCode(User user);

    void deleteOtpMessage(Long chatId, Integer messageId);
    
    void sendProductCreationStart(Long chatId);
    
    void sendProductNamePrompt(Long chatId);
    
    void sendProductDescriptionPrompt(Long chatId);
    
    void sendProductBrandPrompt(Long chatId);
    
    void sendProductImagePrompt(Long chatId, int currentCount);
    
    void sendCategorySelection(Long chatId);
    
    void sendSizeSelection(Long chatId);
    
    void sendProductConfirmation(Long chatId, String productInfo);
    
    void sendProductSavedSuccess(Long chatId);
    
    void sendProductCreationCancelled(Long chatId);
    
    void sendProductSizeQuantityPrompt(Long chatId, org.exp.primeapp.botauth.models.ProductCreationState state);
    
    void sendCategoryCreationStart(Long chatId);
    
    void sendCategoryNamePrompt(Long chatId);
    
    void sendSpotlightNamePrompt(Long chatId);
    
    void sendCategoryConfirmation(Long chatId, String categoryInfo);
    
    void sendCategorySavedSuccess(Long chatId);
    
    void sendCategoryCreationCancelled(Long chatId);
}
