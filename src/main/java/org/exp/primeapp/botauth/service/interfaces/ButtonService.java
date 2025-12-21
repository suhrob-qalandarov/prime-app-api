package org.exp.primeapp.botauth.service.interfaces;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.enums.Size;

import java.util.List;

public interface ButtonService {
    Keyboard sendShareContactBtn();
    
    Keyboard createAdminMainReplyKeyboard();
    
    Keyboard createAdminCancelReplyKeyboard();

    InlineKeyboardMarkup sendRenewCodeBtn();
    
    InlineKeyboardMarkup createCategoryButtons(List<Category> categories);
    
    InlineKeyboardMarkup createSizeButtons(List<Size> allSizes, List<Size> selectedSizes);
    
    InlineKeyboardMarkup createConfirmationButtons();
    
    InlineKeyboardMarkup createContinueOrFinishImageButtons();
    
    InlineKeyboardMarkup createNextStepImageButton();
    
    InlineKeyboardMarkup createAddProductButton();
    
    InlineKeyboardMarkup createAdminMainMenuButtons();
    
    InlineKeyboardMarkup createProductMenuButtons();
    
    InlineKeyboardMarkup createCategoryMenuButtons();
    
    InlineKeyboardMarkup createSpotlightNameButtons();
    
    InlineKeyboardMarkup createCategoryConfirmationButtons();
    
    InlineKeyboardMarkup createSetAdminButton();
    
    InlineKeyboardMarkup createUserRoleButtons(boolean canSetAdmin, boolean canSetSuperAdmin, Long userId);
}
