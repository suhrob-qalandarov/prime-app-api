package org.exp.primeapp.botadmin.service.interfaces;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.enums.Size;

import java.util.List;

public interface AdminButtonService {
    
    Keyboard sendShareContactBtn();
    
    Keyboard createAdminMainReplyKeyboard();
    
    Keyboard createAdminCancelReplyKeyboard();
    
    Keyboard createProductReplyKeyboard();
    
    Keyboard createProductCreationCancelReplyKeyboard();
    
    Keyboard createProductConfirmationReplyKeyboard();
    
    InlineKeyboardMarkup createAdminMainMenuButtons();
    
    InlineKeyboardMarkup createProductMenuButtons();
    
    InlineKeyboardMarkup createCategoryMenuButtons();
    
    InlineKeyboardMarkup createCategoryButtons(List<Category> categories);
    
    InlineKeyboardMarkup createSizeButtons(List<Size> allSizes, List<Size> selectedSizes);
    
    InlineKeyboardMarkup createConfirmationButtons();
    
    InlineKeyboardMarkup createCategoryConfirmationButtons();
    
    InlineKeyboardMarkup createSpotlightNameButtons();
    
    InlineKeyboardMarkup createSpotlightNameButtonsWithBack();
    
    InlineKeyboardMarkup createSetAdminButton();
    
    InlineKeyboardMarkup createUserRoleButtons(boolean canSetAdmin, boolean canSetSuperAdmin, Long userId);
    
    InlineKeyboardMarkup createNextStepButton();
    
    InlineKeyboardMarkup createSkipAdditionalImagesButton();
    
    InlineKeyboardMarkup createColorButtons();
    
    InlineKeyboardMarkup createBackButton(String step);
    
    InlineKeyboardMarkup addBackButton(InlineKeyboardMarkup original, String step);
    
    InlineKeyboardMarkup createNextStepImageButton();
}

