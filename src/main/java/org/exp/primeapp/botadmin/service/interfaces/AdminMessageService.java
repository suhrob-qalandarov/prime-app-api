package org.exp.primeapp.botadmin.service.interfaces;

import org.exp.primeapp.models.entities.User;

public interface AdminMessageService {

    void sendStartMsgForAdmin(Long chatId, String firstName);
    
    void sendAccessDeniedMessage(Long chatId, String userBotUsername);
    
    void sendAdminMenu(Long chatId, String firstName);
    
    void sendAdminMenuWithCancel(Long chatId);
    
    void sendAdminSectionMessage(Long chatId, String sectionName);
    
    void sendProductCreationStart(Long chatId);
    
    Integer sendProductNamePrompt(Long chatId);
    
    Integer sendProductDescriptionPrompt(Long chatId);
    
    Integer sendProductBrandPrompt(Long chatId);
    
    Integer sendProductColorPrompt(Long chatId);
    
    Integer sendMainImagePrompt(Long chatId);
    
    void sendAdditionalImagesPrompt(Long chatId, int currentCount);
    
    void sendProductImagePrompt(Long chatId, int currentCount);
    
    void sendImageSavedSuccess(Long chatId, int currentCount, int remaining);
    
    void sendSpotlightNamePromptForProduct(Long chatId);
    
    void sendCategorySelection(Long chatId);
    
    void sendSizeSelection(Long chatId);
    
    Integer sendProductPricePrompt(Long chatId);
    
    void sendProductConfirmation(Long chatId, String productInfo, org.exp.primeapp.botadmin.models.ProductCreationState state);
    
    void sendProductSavedSuccess(Long chatId);
    
    void sendProductCreationCancelled(Long chatId);
    
    void sendProductSizeQuantityPrompt(Long chatId, org.exp.primeapp.botadmin.models.ProductCreationState state);
    
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
    
    void sendNoCategoriesMessage(Long chatId);
    
    void sendSimpleMessage(Long chatId, String message);
}

