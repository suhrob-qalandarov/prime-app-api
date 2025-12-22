package org.exp.primeapp.botadmin.service.interfaces;

import org.exp.primeapp.botadmin.models.CategoryCreationState;

public interface BotCategoryService {
    
    void startCategoryCreation(Long userId);
    
    CategoryCreationState getCategoryCreationState(Long userId);
    
    void clearCategoryCreationState(Long userId);
    
    void handleCategoryName(Long userId, String name);
    
    void handleSpotlightName(Long userId, String spotlightName);
    
    void confirmAndSaveCategory(Long userId);
    
    void cancelCategoryCreation(Long userId);
}

