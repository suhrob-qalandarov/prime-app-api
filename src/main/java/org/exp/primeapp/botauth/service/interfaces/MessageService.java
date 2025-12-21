package org.exp.primeapp.botauth.service.interfaces;

import org.exp.primeapp.models.entities.User;

public interface MessageService {

    void sendStartMsg(Long chatId, String firstName);

    void sendStartMsgForAdmin(Long chatId, String firstName);
    
    void sendAdminMenu(Long chatId, String firstName);
    
    void sendAdminMenuWithCancel(Long chatId);
    
    void sendAdminSectionMessage(Long chatId, String sectionName);

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
    
    void sendProductColorPrompt(Long chatId);
    
    void sendProductImagePrompt(Long chatId, int currentCount);
    
    void sendMainImagePrompt(Long chatId);
    
    void sendAdditionalImagesPrompt(Long chatId, int currentCount);
    
    void sendImageSavedSuccess(Long chatId, int currentCount, int remaining);
    
    void sendImagesCompleted(Long chatId, int totalCount);
    
    void sendSpotlightNamePromptForProduct(Long chatId);
    
    void sendCategorySelection(Long chatId);
    
    void sendSizeSelection(Long chatId);
    
    void sendProductPricePrompt(Long chatId);
    
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
    
    void sendUsersStatistics(Long chatId, long totalCount, long adminCount, long superAdminCount, boolean isSuperAdmin);
    
    void sendPhoneNumberPrompt(Long chatId);
    
    void sendUserNotFound(Long chatId);
    
    void sendUserInfo(Long chatId, User user, boolean canSetAdmin, boolean canSetSuperAdmin);
    
    void sendRoleAddedSuccess(Long chatId, String roleName);
}
