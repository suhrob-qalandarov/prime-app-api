package org.exp.primeapp.botauth.handle;

import com.pengrad.telegrambot.model.Contact;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botauth.models.CategoryCreationState;
import org.exp.primeapp.botauth.models.ProductCreationState;
import org.exp.primeapp.botauth.service.interfaces.BotCategoryService;
import org.exp.primeapp.botauth.service.interfaces.BotProductService;
import org.exp.primeapp.botauth.service.interfaces.BotUserService;
import org.exp.primeapp.botauth.service.interfaces.MessageService;
import org.exp.primeapp.botauth.service.interfaces.UserService;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.models.enums.Size;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler implements Consumer<Message> {

    private final MessageService messageService;
    private final UserService userService;
    private final BotProductService botProductService;
    private final BotCategoryService botCategoryService;
    private final BotUserService botUserService;

    @Override
    public void accept(Message message) {
        try {
            log.debug("Received message from chatId: {}, text: {}", message.chat().id(), message.text());
            
            String text = message.text();
            User user = userService.getOrCreateUser(message.from());
            Long userId = user.getId();
            Long chatId = user.getTelegramId();
            
            log.debug("Processing message for user: {} (chatId: {}), text: {}", userId, chatId, text);

            if (message.contact() != null) {
                Contact contact = message.contact();
                messageService.removeKeyboardAndSendMsg(chatId);
                userService.updateUserPhoneById(chatId, contact.phoneNumber());
                
                // Check if user is admin
                boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                        .anyMatch(role -> role.getName() != null && 
                                (role.getName().equals("ROLE_ADMIN") || 
                                 role.getName().equals("ROLE_SUPER_ADMIN")));
                
                if (isAdmin) {
                    // Admin - send admin menu
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "Admin";
                    messageService.sendAdminMenu(chatId, firstName);
                } else {
                    // Regular user - send code
                    messageService.sendCode(user);
                }

            } else if (text != null && text.trim().startsWith("/start")) {
                log.info("Processing /start command from chatId: {}", chatId);
                String firstName = user.getFirstName() != null ? user.getFirstName() : "Foydalanuvchi";
                
                // Check if user has phone number (already registered)
                boolean hasPhone = user.getPhone() != null && !user.getPhone().trim().isEmpty();
                
                // Check if user is admin or super admin
                boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                        .anyMatch(role -> role.getName() != null && 
                                (role.getName().equals("ROLE_ADMIN") || 
                                 role.getName().equals("ROLE_SUPER_ADMIN")));
                
                if (hasPhone) {
                    // User already has phone
                    if (isAdmin) {
                        // Admin with phone - send admin menu
                        log.info("Admin {} already has phone, sending admin menu", userId);
                        messageService.sendAdminMenu(chatId, firstName);
                    } else {
                        // Regular user with phone - send code page
                        log.info("User {} already has phone, sending code page", userId);
                        messageService.sendCode(user);
                    }
                } else if (isAdmin) {
                    // Admin without phone - ask for contact first
                    log.info("User {} is admin but no phone, asking for contact", userId);
                    messageService.sendStartMsgForAdmin(chatId, firstName);
                } else {
                    // Regular user without phone - ask for contact
                    messageService.sendStartMsg(chatId, firstName);
                }
                log.info("Start message sent to chatId: {}", chatId);

            } else if (text != null && text.trim().equals("/login")) {
                log.info("Processing /login command from chatId: {}", chatId);
                userService.updateOneTimeCode(chatId);
                messageService.sendLoginMsg(chatId);

            } else if (text != null && text.trim().equals("/add_product")) {
                log.info("Processing /add_product command from chatId: {}", chatId);
                botProductService.startProductCreation(userId);
                messageService.sendProductCreationStart(chatId);
                messageService.sendProductNamePrompt(chatId);

            } else if (text != null) {
                // Check if user is admin and handle admin menu buttons
                boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                        .anyMatch(role -> role.getName() != null && 
                                (role.getName().equals("ROLE_ADMIN") || 
                                 role.getName().equals("ROLE_SUPER_ADMIN")));
                
                if (isAdmin) {
                    if (text.equals("📊 Dashboard")) {
                        messageService.sendAdminSectionMessage(chatId, "Dashboard");
                        return;
                    } else if (text.equals("📦 Buyurtmalar")) {
                        messageService.sendAdminSectionMessage(chatId, "Buyurtmalar");
                        return;
                    } else if (text.equals("🛍️ Mahsulotlar")) {
                        messageService.sendAdminSectionMessage(chatId, "Mahsulotlar");
                        return;
                    } else if (text.equals("📂 Kategoriyalar")) {
                        messageService.sendAdminSectionMessage(chatId, "Kategoriyalar");
                        return;
                    } else if (text.equals("👥 Foydalanuvchilar")) {
                        // Check if user is super admin
                        boolean isSuperAdmin = user.getRoles() != null && user.getRoles().stream()
                                .anyMatch(role -> role.getName() != null && 
                                        role.getName().equals("ROLE_SUPER_ADMIN"));
                        
                        long[] counts = botUserService.getUserCounts();
                        messageService.sendUsersStatistics(chatId, counts[0], counts[1], counts[2], isSuperAdmin);
                        return;
                    } else if (text.equals("❌ Bekor qilish")) {
                        // Cancel all active states
                        botUserService.setUserSearchState(userId, false);
                        
                        // Cancel product creation if active
                        ProductCreationState productState = botProductService.getProductCreationState(userId);
                        if (productState != null) {
                            botProductService.cancelProductCreation(userId);
                        }
                        
                        // Cancel category creation if active
                        CategoryCreationState categoryState = botCategoryService.getCategoryCreationState(userId);
                        if (categoryState != null) {
                            botCategoryService.cancelCategoryCreation(userId);
                        }
                        
                        // Return to main menu
                        String firstName = user.getFirstName() != null ? user.getFirstName() : "Admin";
                        messageService.sendAdminMenu(chatId, firstName);
                        return;
                    }
                }
            }
            
            // Check if user is in user search flow
            if (botUserService.getUserSearchState(userId)) {
                handleUserSearchMessage(message, user);
                return;
            }
            
            // Check if user is in category creation flow
            CategoryCreationState categoryState = botCategoryService.getCategoryCreationState(userId);
            if (categoryState != null) {
                handleCategoryCreationMessage(message, user, categoryState);
            } else {
                // Check if user is in product creation flow
                ProductCreationState state = botProductService.getProductCreationState(userId);
                if (state != null) {
                    handleProductCreationMessage(message, user, state);
                } else {
                    log.debug("No handler for message from chatId: {}, text: {}", chatId, text);
                }
            }
        } catch (Exception e) {
            log.error("Error processing message from chatId: {}", message.chat().id(), e);
            try {
                // Try to send error message to user
                Long chatId = message.chat().id();
                messageService.sendStartMsg(chatId, "Foydalanuvchi");
            } catch (Exception ex) {
                log.error("Failed to send error message to chatId: {}", message.chat().id(), ex);
            }
        }
    }

    private void handleProductCreationMessage(Message message, User user, ProductCreationState state) {
        String text = message.text();
        Long chatId = user.getTelegramId();
        Long userId = user.getId();

        // Handle photo messages
        if (message.photo() != null && message.photo().length > 0) {
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
                    // Note: handleProductImage already changed step to WAITING_ADDITIONAL_IMAGES
                    messageService.sendAdditionalImagesPrompt(chatId, currentCount);
                } else if (state.getCurrentStep() == ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES) {
                    // Additional image uploaded
                    int remaining = 3 - currentCount; // Max 3 total, calculate remaining
                    if (currentCount >= 3) {
                        // Max 3 images reached
                        messageService.sendImagesCompleted(chatId, currentCount);
                        state.setCurrentStep(ProductCreationState.Step.WAITING_SPOTLIGHT_NAME);
                        messageService.sendSpotlightNamePromptForProduct(chatId);
                    } else {
                        // Can add more additional images (max 2 additional = 3 total)
                        messageService.sendImageSavedSuccess(chatId, currentCount, remaining);
                    }
                }
            }
            return;
        }

        // Handle text messages
        if (text == null || text.trim().isEmpty()) {
            return;
        }

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
                    // Empty brand - set to empty string
                    botProductService.handleProductBrand(userId, "");
                }
                // After brand, move to color selection step
                state.setCurrentStep(ProductCreationState.Step.WAITING_COLOR);
                messageService.sendProductColorPrompt(chatId);
                break;

            case WAITING_QUANTITIES:
                // Handle quantity input for sizes
                // User should send quantity as a number
                try {
                    Integer quantity = Integer.parseInt(text.trim());
                    if (quantity <= 0) {
                        messageService.sendProductSizeQuantityPrompt(user.getTelegramId(), state);
                        return;
                    }
                    
                    // Find the first size without quantity
                    Size sizeToSet = null;
                    for (Size size : state.getSelectedSizes()) {
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
                        for (Size size : state.getSelectedSizes()) {
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
                    messageService.sendProductConfirmation(user.getTelegramId(), productInfo);
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
            for (Size size : state.getSelectedSizes()) {
                Integer qty = state.getSizeQuantities().getOrDefault(size, 0);
                info.append("  • ").append(size.getLabel()).append(": ").append(qty).append(" ta\n");
            }
        }
        return info.toString();
    }

    private void handleCategoryCreationMessage(Message message, User user, CategoryCreationState state) {
        String text = message.text();
        Long chatId = user.getTelegramId();
        Long userId = user.getId();

        // Handle text messages only
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        switch (state.getCurrentStep()) {
            case WAITING_NAME:
                botCategoryService.handleCategoryName(userId, text);
                messageService.sendSpotlightNamePrompt(chatId);
                break;

            default:
                break;
        }
    }
    
    private void handleUserSearchMessage(Message message, User user) {
        String text = message.text();
        Long chatId = user.getTelegramId();
        Long userId = user.getId();
        
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Search user by phone number
        User foundUser = botUserService.findUserByPhone(text.trim());
        
        if (foundUser == null) {
            messageService.sendUserNotFound(chatId);
            botUserService.setUserSearchState(userId, false);
        } else {
            // Check if user can set admin or super admin
            boolean canSetAdmin = !botUserService.hasRole(foundUser, "ROLE_ADMIN") && 
                                 !botUserService.hasRole(foundUser, "ROLE_SUPER_ADMIN");
            boolean canSetSuperAdmin = !botUserService.hasRole(foundUser, "ROLE_SUPER_ADMIN");
            
            messageService.sendUserInfo(chatId, foundUser, canSetAdmin, canSetSuperAdmin);
            botUserService.setUserSearchState(userId, false);
        }
    }
}
