package org.exp.primeapp.botauth.handle;

import com.pengrad.telegrambot.model.Contact;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botauth.models.ProductCreationState;
import org.exp.primeapp.botauth.service.interfaces.BotProductService;
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
                messageService.sendCode(user);
                userService.updateUserPhoneById(chatId, contact.phoneNumber());

            } else if (text != null && text.trim().startsWith("/start")) {
                log.info("Processing /start command from chatId: {}", chatId);
                String firstName = user.getFirstName() != null ? user.getFirstName() : "Foydalanuvchi";
                
                // Check if user is admin or super admin
                boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                        .anyMatch(role -> role.getName() != null && 
                                (role.getName().equals("ROLE_ADMIN") || 
                                 role.getName().equals("ROLE_SUPER_ADMIN")));
                
                if (isAdmin) {
                    log.info("User {} is admin, sending admin start message", userId);
                    messageService.sendStartMsgForAdmin(chatId, firstName);
                } else {
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
            if (state.getCurrentStep() == ProductCreationState.Step.WAITING_IMAGES) {
                // Get the largest photo
                PhotoSize[] photos = message.photo();
                PhotoSize largestPhoto = photos[photos.length - 1];
                String fileId = largestPhoto.fileId();
                
                botProductService.handleProductImage(userId, fileId);
                
                int currentCount = state.getAttachmentUrls() != null ? state.getAttachmentUrls().size() : 0;
                if (state.hasMinimumImages() && state.canAddMoreImages()) {
                    messageService.sendProductImagePrompt(chatId, currentCount);
                } else if (state.hasMinimumImages()) {
                    // Max 3 images reached, move to next step
                    state.setCurrentStep(ProductCreationState.Step.WAITING_CATEGORY);
                    messageService.sendCategorySelection(chatId);
                    // Category buttons will be sent in callback handler
                } else {
                    messageService.sendProductImagePrompt(chatId, currentCount);
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
                botProductService.handleProductBrand(userId, text);
                state.setCurrentStep(ProductCreationState.Step.WAITING_IMAGES);
                messageService.sendProductImagePrompt(chatId, 0);
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
                            // All quantities set, show confirmation
                            state.setCurrentStep(ProductCreationState.Step.CONFIRMATION);
                            String productInfo = buildProductInfo(state);
                            messageService.sendProductConfirmation(user.getTelegramId(), productInfo);
                        } else {
                            // Ask for next size quantity
                            messageService.sendProductSizeQuantityPrompt(user.getTelegramId(), state);
                        }
                    } else {
                        // All quantities set, show confirmation
                        state.setCurrentStep(ProductCreationState.Step.CONFIRMATION);
                        String productInfo = buildProductInfo(state);
                        messageService.sendProductConfirmation(user.getTelegramId(), productInfo);
                    }
                } catch (NumberFormatException e) {
                    messageService.sendProductSizeQuantityPrompt(user.getTelegramId(), state);
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
}
