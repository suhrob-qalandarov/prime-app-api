package org.exp.primeapp.botauth.service.interfaces;

import org.exp.primeapp.botauth.models.ProductCreationState;

import java.util.List;

public interface BotProductService {
    
    void startProductCreation(Long userId);
    
    ProductCreationState getProductCreationState(Long userId);
    
    void clearProductCreationState(Long userId);
    
    void handleProductName(Long userId, String name);
    
    void handleProductDescription(Long userId, String description);
    
    void handleProductBrand(Long userId, String brand);
    
    void handleProductImage(Long userId, String fileId);
    
    void handleSpotlightNameSelection(Long userId, String spotlightName);
    
    void handleCategorySelection(Long userId, Long categoryId);
    
    void handleSizeSelection(Long userId, String sizeName);
    
    void handleSizeQuantity(Long userId, String sizeName, Integer quantity);
    
    void confirmAndSaveProduct(Long userId);
    
    void cancelProductCreation(Long userId);
    
    List<org.exp.primeapp.models.entities.Category> getAllCategories();
    
    List<org.exp.primeapp.models.entities.Category> getCategoriesBySpotlightName(String spotlightName);
}

