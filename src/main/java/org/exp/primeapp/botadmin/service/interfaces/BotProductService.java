package org.exp.primeapp.botadmin.service.interfaces;

import org.exp.primeapp.botadmin.models.ProductCreationState;

import java.util.List;

public interface BotProductService {
    
    void startProductCreation(Long userId);
    
    ProductCreationState getProductCreationState(Long userId);
    
    void clearProductCreationState(Long userId);
    
    void handleProductName(Long userId, String name);
    
    void handleProductDescription(Long userId, String description);
    
    void handleProductBrand(Long userId, String brand);
    
    void handleProductColor(Long userId, String colorName, String colorHex);
    
    void handleProductImage(Long userId, String fileId);
    
    void handleSpotlightNameSelection(Long userId, String spotlightName);
    
    void handleCategorySelection(Long userId, Long categoryId);
    
    void handleSizeSelection(Long userId, String sizeName);
    
    void handleSizeQuantity(Long userId, String sizeName, Integer quantity);
    
    void handleProductPrice(Long userId, String priceText);
    
    void confirmAndSaveProduct(Long userId);
    
    void cancelProductCreation(Long userId);
    
    void clearMainImage(Long userId);
    
    void clearAdditionalImages(Long userId);
    
    void goToPreviousStep(Long userId);
    
    List<org.exp.primeapp.models.entities.Category> getAllCategories();
    
    List<org.exp.primeapp.models.entities.Category> getCategoriesBySpotlightName(String spotlightName);
}

