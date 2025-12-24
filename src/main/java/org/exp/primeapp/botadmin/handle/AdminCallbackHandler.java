package org.exp.primeapp.botadmin.handle;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botadmin.models.CategoryCreationState;
import org.exp.primeapp.botadmin.models.ProductCreationState;
import org.exp.primeapp.botadmin.service.interfaces.BotCategoryService;
import org.exp.primeapp.botadmin.service.interfaces.BotProductService;
import org.exp.primeapp.botadmin.service.interfaces.BotUserService;
import org.exp.primeapp.botadmin.service.interfaces.AdminButtonService;
import org.exp.primeapp.botadmin.service.interfaces.AdminMessageService;
import org.exp.primeapp.botuser.service.interfaces.UserService;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.models.enums.Size;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class AdminCallbackHandler implements Consumer<CallbackQuery> {

    private final UserService userService;
    private final AdminMessageService messageService;
    private final BotProductService botProductService;
    private final BotCategoryService botCategoryService;
    private final BotUserService botUserService;
    private final AdminButtonService buttonService;
    private final TelegramBot telegramBot;
    private final TelegramBot userBot;

    public AdminCallbackHandler(UserService userService,
                                 AdminMessageService messageService,
                                 BotProductService botProductService,
                                 BotCategoryService botCategoryService,
                                 BotUserService botUserService,
                                 AdminButtonService buttonService,
                                 @Qualifier("adminBot") TelegramBot telegramBot,
                                 @Qualifier("userBot") TelegramBot userBot) {
        this.userService = userService;
        this.messageService = messageService;
        this.botProductService = botProductService;
        this.botCategoryService = botCategoryService;
        this.botUserService = botUserService;
        this.buttonService = buttonService;
        this.telegramBot = telegramBot;
        this.userBot = userBot;
    }

    private boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() != null && 
                        (role.getName().equals("ROLE_ADMIN") || 
                         role.getName().equals("ROLE_SUPER_ADMIN")));
    }

    private String getUserBotUsername() {
        try {
            if (userBot != null) {
                var me = userBot.execute(new GetMe());
                if (me.isOk() && me.user() != null) {
                    return me.user().username();
                }
            }
        } catch (Exception e) {
            log.error("Error getting user bot username: {}", e.getMessage());
        }
        return "prime77Robot"; // Default fallback
    }

    @Override
    public void accept(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        String callbackId = callbackQuery.id();
        User user = userService.getOrCreateUser(callbackQuery.from());
        Long userId = user.getId();
        Long chatId = user.getTelegramId();

        // Check if user is admin - if not, send access denied message
        if (!isAdmin(user)) {
            log.warn("Non-admin user {} tried to access admin bot via callback", userId);
            String userBotUsername = getUserBotUsername();
            messageService.sendAccessDeniedMessage(chatId, userBotUsername);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Kirish mumkin emas!").showAlert(true));
            return;
        }

        // Handle admin menu callbacks
        if (data.equals("admin_menu_product")) {
            com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
            Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
            if (messageId != null) {
                telegramBot.execute(new EditMessageText(chatId, messageId,
                        "🛍️ <b>Product bo'limi</b>\n\nQuyidagi amallardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createProductMenuButtons())
                );
            } else {
                telegramBot.execute(new SendMessage(chatId,
                        "🛍️ <b>Product bo'limi</b>\n\nQuyidagi amallardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createProductMenuButtons())
                );
            }
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Product bo'limi"));
            return;
        }

        if (data.equals("admin_menu_category")) {
            com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
            Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
            if (messageId != null) {
                telegramBot.execute(new EditMessageText(chatId, messageId,
                        "📂 <b>Category bo'limi</b>\n\nQuyidagi amallardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createCategoryMenuButtons())
                );
            } else {
                telegramBot.execute(new SendMessage(chatId,
                        "📂 <b>Category bo'limi</b>\n\nQuyidagi amallardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createCategoryMenuButtons())
                );
            }
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Category bo'limi"));
            return;
        }

        if (data.equals("admin_menu_orders")) {
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Orders bo'limi keyinroq qo'shiladi")
                    .showAlert(true));
            return;
        }

        if (data.equals("admin_menu_back")) {
            com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
            Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
            if (messageId != null) {
                telegramBot.execute(new EditMessageText(chatId, messageId,
                        "👨‍💼 <b>Xush kelibsiz, Admin!</b>\n\n" +
                        "Quyidagi bo'limlardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createAdminMainMenuButtons())
                );
            } else {
                telegramBot.execute(new SendMessage(chatId,
                        "👨‍💼 <b>Xush kelibsiz, Admin!</b>\n\n" +
                        "Quyidagi bo'limlardan birini tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createAdminMainMenuButtons())
                );
            }
            
            // Cancel category creation if active
            CategoryCreationState categoryState = botCategoryService.getCategoryCreationState(userId);
            if (categoryState != null) {
                botCategoryService.cancelCategoryCreation(userId);
            }
            
            // Return to main menu
            String firstName = user.getFirstName() != null ? user.getFirstName() : "Admin";
            messageService.sendAdminMenu(chatId, firstName);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Asosiy menyu"));
            return;
        }

        if (data.equals("admin_product_edit")) {
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Product tahrirlash funksiyasi keyinroq qo'shiladi")
                    .showAlert(true));
            return;
        }

        if (data.equals("admin_product_add_income")) {
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Product income qo'shish funksiyasi keyinroq qo'shiladi")
                    .showAlert(true));
            return;
        }

        if (data.equals("admin_product_add_outcome")) {
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Product outcome qo'shish funksiyasi keyinroq qo'shiladi")
                    .showAlert(true));
            return;
        }

        if (data.equals("admin_category_add")) {
            botCategoryService.startCategoryCreation(userId);
            messageService.sendCategoryCreationStart(chatId);
            messageService.sendCategoryNamePrompt(chatId);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Kategoriya qo'shish boshlandi"));
            return;
        }

        if (data.startsWith("spotlight_")) {
            String spotlightName = data.replace("spotlight_", "");
            // Map callback data to actual spotlight name
            String actualSpotlightName = switch (spotlightName) {
                case "tepa_kiyimlar" -> "Tepa kiyimlar";
                case "shimlar" -> "Shimlar";
                case "oyoq_kiyimlar" -> "Oyoq kiyimlar";
                case "aksessuarlar" -> "Aksessuarlar";
                default -> spotlightName;
            };
            
            // Check if it's for product creation or category creation
            ProductCreationState productState = botProductService.getProductCreationState(userId);
            if (productState != null && productState.getCurrentStep() == ProductCreationState.Step.WAITING_SPOTLIGHT_NAME) {
                // Product creation flow
                botProductService.handleSpotlightNameSelection(userId, actualSpotlightName);
                List<Category> categories = botProductService.getCategoriesBySpotlightName(actualSpotlightName);
                
                // Check if categories exist
                if (categories == null || categories.isEmpty()) {
                    // No categories found - cancel product creation and return to main menu
                    botProductService.cancelProductCreation(userId);
                    messageService.sendNoCategoriesMessage(chatId);
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "Admin";
                    messageService.sendAdminMenu(chatId, firstName);
                    telegramBot.execute(new AnswerCallbackQuery(callbackId)
                            .text("Kategoriya mavjud emas")
                            .showAlert(true));
                    return;
                }
                
                messageService.sendCategorySelection(chatId);
                telegramBot.execute(new SendMessage(chatId, "📂 Kategoriyani tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.createCategoryButtons(categories))
                );
                telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Toifa tanlandi"));
            } else {
                // Category creation flow
                botCategoryService.handleSpotlightName(userId, actualSpotlightName);
                CategoryCreationState categoryState = botCategoryService.getCategoryCreationState(userId);
                if (categoryState != null) {
                    String categoryInfo = buildCategoryInfo(categoryState);
                    messageService.sendCategoryConfirmation(chatId, categoryInfo);
                }
                telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Spotlight nomi tanlandi"));
            }
            return;
        }

        if (data.equals("confirm_category")) {
            try {
                CategoryCreationState state = botCategoryService.getCategoryCreationState(userId);
                if (state == null) {
                    telegramBot.execute(new AnswerCallbackQuery(callbackId)
                            .text("Kategoriya qo'shish jarayoni topilmadi")
                            .showAlert(true));
                    return;
                }
                
                botCategoryService.confirmAndSaveCategory(userId);
                messageService.sendCategorySavedSuccess(chatId);
                telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Kategoriya qo'shildi"));
            } catch (Exception e) {
                log.error("Error confirming category: {}", e.getMessage(), e);
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Xatolik: " + e.getMessage())
                        .showAlert(true));
            }
            return;
        }

        if (data.equals("cancel_category")) {
            botCategoryService.cancelCategoryCreation(userId);
            messageService.sendCategoryCreationCancelled(chatId);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Bekor qilindi"));
            return;
        }

        if (data.equals("admin_category_edit")) {
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Category tahrirlash funksiyasi keyinroq qo'shiladi")
                    .showAlert(true));
            return;
        }

        // renew_code is for user bot only, skip it in admin bot
        if (data.equals("renew_code")) {
            return;
        }

        if (data.equals("add_product")) {
            botProductService.startProductCreation(userId);
            messageService.sendProductCreationStart(chatId);
            messageService.sendProductNamePrompt(chatId);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Mahsulot qo'shish boshlandi"));
            return;
        }
        
        // Handle user role callbacks (before product creation state check)
        if (data.equals("set_admin_search")) {
            // Check if user is super admin
            boolean isSuperAdmin = user.getRoles() != null && user.getRoles().stream()
                    .anyMatch(role -> role.getName() != null && 
                            role.getName().equals("ROLE_SUPER_ADMIN"));
            
            if (!isSuperAdmin) {
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Faqat Super Admin bu funksiyani ishlatishi mumkin")
                        .showAlert(true));
                return;
            }
            
            messageService.sendPhoneNumberPrompt(chatId);
            botUserService.setUserSearchState(userId, true);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Telefon raqam kiriting"));
            return;
        }
        
        if (data.startsWith("set_admin_")) {
            Long targetUserId = Long.parseLong(data.substring("set_admin_".length()));
            
            try {
                botUserService.addRoleToUser(targetUserId, "ROLE_ADMIN");
                messageService.sendRoleAddedSuccess(chatId, "ROLE_ADMIN");
                telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Admin role qo'shildi"));
            } catch (Exception e) {
                log.error("Failed to add admin role to user: {}", targetUserId, e);
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Xatolik: " + e.getMessage())
                        .showAlert(true));
            }
            return;
        }
        
        if (data.startsWith("set_super_admin_")) {
            // Check if user is super admin
            boolean isSuperAdmin = user.getRoles() != null && user.getRoles().stream()
                    .anyMatch(role -> role.getName() != null && 
                            role.getName().equals("ROLE_SUPER_ADMIN"));
            
            if (!isSuperAdmin) {
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Faqat Super Admin bu funksiyani ishlatishi mumkin")
                        .showAlert(true));
                return;
            }
            
            Long targetUserId = Long.parseLong(data.substring("set_super_admin_".length()));
            
            try {
                botUserService.addRoleToUser(targetUserId, "ROLE_SUPER_ADMIN");
                messageService.sendRoleAddedSuccess(chatId, "ROLE_SUPER_ADMIN");
                telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Super Admin role qo'shildi"));
            } catch (Exception e) {
                log.error("Failed to add super admin role to user: {}", targetUserId, e);
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Xatolik: " + e.getMessage())
                        .showAlert(true));
            }
            return;
        }

        // Handle product creation callbacks
        ProductCreationState state = botProductService.getProductCreationState(userId);
        if (state == null) {
            // Don't show error for admin menu callbacks and user role callbacks
            if (!data.startsWith("admin_") && !data.startsWith("set_")) {
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Mahsulot qo'shish jarayoni topilmadi. /add_product bilan boshlang.")
                        .showAlert(true));
            }
            return;
        }

        // Handle back button callbacks
        if (data.startsWith("back_to_")) {
            String stepName = data.replace("back_to_", "");
            ProductCreationState.Step targetStep = null;
            
            try {
                targetStep = ProductCreationState.Step.valueOf(stepName);
            } catch (IllegalArgumentException e) {
                log.error("Invalid step name: {}", stepName);
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Xatolik: Noto'g'ri qadam")
                        .showAlert(true));
                return;
            }
            
            // Edit current message to remove inline buttons
            com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
            Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
            if (messageId != null) {
                // Get current message text
                String currentText = callbackMessage.text();
                if (currentText != null) {
                    // Edit message to remove inline buttons
                    telegramBot.execute(new EditMessageText(chatId, messageId, currentText)
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
                    );
                }
            }
            
            // Set the state to target step directly
            if (state != null) {
                state.setCurrentStep(targetStep);
            }
            
            // Send appropriate prompt based on the step we're going back to
            switch (targetStep) {
                case WAITING_NAME:
                    messageService.sendProductNamePrompt(chatId);
                    break;
                case WAITING_DESCRIPTION:
                    messageService.sendProductDescriptionPrompt(chatId);
                    break;
                case WAITING_BRAND:
                    messageService.sendProductBrandPrompt(chatId);
                    break;
                case WAITING_COLOR:
                    messageService.sendProductColorPrompt(chatId);
                    break;
                default:
                    log.warn("No handler for back to step: {}", targetStep);
                    break;
            }
            
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Orqaga qaytildi"));
            return;
        }

        // Handle skip brand callback
        if (data.equals("skip_brand")) {
            // Skip brand - set empty brand
            botProductService.handleProductBrand(userId, "");
            
            // Edit message to show skipped
            com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
            Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
            if (messageId != null) {
                telegramBot.execute(new EditMessageText(chatId, messageId,
                        "🏷️ <b>3/9</b> Brend nomi: (O'tkazib yuborildi)")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
                );
            }
            
            // Move to color selection step
            state.setCurrentStep(ProductCreationState.Step.WAITING_COLOR);
            messageService.sendProductColorPrompt(chatId);
            
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Brend o'tkazib yuborildi"));
            return;
        }

        // Handle color selection callbacks
        if (data.startsWith("select_color_")) {
            // Parse color name and hex from callback data
            // Format: "select_color_ColorName_#HexCode"
            String colorData = data.replace("select_color_", "");
            // Split by last underscore to separate name and hex
            int lastUnderscoreIndex = colorData.lastIndexOf("_");
            if (lastUnderscoreIndex > 0 && lastUnderscoreIndex < colorData.length() - 1) {
                String colorName = colorData.substring(0, lastUnderscoreIndex);
                String colorHex = colorData.substring(lastUnderscoreIndex + 1);
                
                log.debug("Parsed color selection - name: {}, hex: {}", colorName, colorHex);
                
                botProductService.handleProductColor(userId, colorName, colorHex);
                
                // Edit message to show selected color
                com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
                Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
                if (messageId != null) {
                    telegramBot.execute(new EditMessageText(chatId, messageId,
                            "🎨 <b>4/9</b> Rang tanlandi: " + colorName)
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
                    );
                }
                
                // Move to main image step
                state.setCurrentStep(ProductCreationState.Step.WAITING_MAIN_IMAGE);
                messageService.sendMainImagePrompt(chatId);
                
                telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Rang tanlandi"));
            } else {
                log.error("Failed to parse color data: {}", colorData);
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Xatolik: Rang ma'lumotlarini parse qilishda muammo")
                        .showAlert(true));
            }
            return;
        }

        if (data.equals("skip_color")) {
            // Skip color selection - set default values
            botProductService.handleProductColor(userId, "N/A", "#000000");
            
            // Edit message to show skipped
            com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
            Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
            if (messageId != null) {
                telegramBot.execute(new EditMessageText(chatId, messageId,
                        "🎨 <b>4/9</b> Rang tanlash: (O'tkazib yuborildi)")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
                );
            }
            
            // Move to main image step
            state.setCurrentStep(ProductCreationState.Step.WAITING_MAIN_IMAGE);
            messageService.sendMainImagePrompt(chatId);
            
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Rang tanlash o'tkazib yuborildi"));
            return;
        }

        if (data.startsWith("select_category_")) {
            Long categoryId = Long.parseLong(data.replace("select_category_", ""));
            botProductService.handleCategorySelection(userId, categoryId);
            
            // After category selected, show sizes
            state.setCurrentStep(ProductCreationState.Step.WAITING_SIZES);
            messageService.sendSizeSelection(chatId);
            List<Size> allSizes = List.of(Size.values());
            telegramBot.execute(new SendMessage(chatId, "📏 O'lchamlarni tanlang (bir nechtasini tanlash mumkin):")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createSizeButtons(allSizes, state.getSelectedSizes()))
            );
            
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Kategoriya tanlandi"));

        } else if (data.startsWith("toggle_size_")) {
            String sizeName = data.replace("toggle_size_", "");
            botProductService.handleSizeSelection(userId, sizeName);
            
            // Update size buttons
            List<Size> allSizes = List.of(Size.values());
            com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
            Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
            if (messageId != null) {
                telegramBot.execute(new EditMessageText(chatId, messageId,
                    "📏 O'lchamlarni tanlang:")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createSizeButtons(allSizes, state.getSelectedSizes()))
                );
            }
            
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("O'lcham tanlandi"));

        } else if (data.equals("continue_sizes")) {
            if (state.getSelectedSizes().isEmpty()) {
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Kamida bitta o'lcham tanlang")
                        .showAlert(true));
                return;
            }
            
            // Move to quantity input step
            state.setCurrentStep(ProductCreationState.Step.WAITING_QUANTITIES);
            StringBuilder quantityPrompt = new StringBuilder("📊 Har bir o'lcham uchun miqdorni kiriting:\n\n");
            for (Size size : state.getSelectedSizes()) {
                quantityPrompt.append(size.getLabel()).append(": /qty_").append(size.name()).append("\n");
            }
            
            telegramBot.execute(new SendMessage(chatId, quantityPrompt.toString())
                    .parseMode(ParseMode.HTML)
            );
            
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("O'lchamlar tanlandi"));

        } else if (data.startsWith("qty_")) {
            // This will be handled when user sends a number message
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Miqdorni raqam sifatida yuboring"));

        } else if (data.equals("add_more_image")) {
            if (state.canAddMoreImages()) {
                state.setCurrentStep(ProductCreationState.Step.WAITING_ADDITIONAL_IMAGES);
                int currentCount = state.getAttachmentUrls() != null ? state.getAttachmentUrls().size() : 0;
                messageService.sendAdditionalImagesPrompt(chatId, currentCount);
            } else {
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Maksimum 3 ta rasm yuklash mumkin")
                        .showAlert(true));
            }

        } else if (data.equals("continue_images")) {
            if (!state.hasMinimumImages()) {
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Kamida 1 ta rasm yuklashingiz kerak")
                        .showAlert(true));
                return;
            }
            
            int totalCount = state.getAttachmentUrls() != null ? state.getAttachmentUrls().size() : 0;
            messageService.sendImagesCompleted(chatId, totalCount);
            
            state.setCurrentStep(ProductCreationState.Step.WAITING_SPOTLIGHT_NAME);
            // Send spotlight name prompt in separate message
            messageService.sendSpotlightNamePromptForProduct(chatId);
            
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Keyingi qadamga o'tildi"));

        } else if (data.equals("confirm_product")) {
            try {
                // Build product info string
                StringBuilder productInfo = new StringBuilder();
                productInfo.append("<b>Nomi:</b> ").append(state.getName()).append("\n");
                productInfo.append("<b>Brend:</b> ").append(state.getBrand()).append("\n");
                productInfo.append("<b>Tavsif:</b> ").append(state.getDescription()).append("\n");
                productInfo.append("<b>Kategoriya:</b> ").append(state.getCategory().getName()).append("\n");
                productInfo.append("<b>Rasmlar:</b> ").append(state.getAttachmentUrls().size()).append(" ta\n");
                productInfo.append("<b>O'lchamlar:</b>\n");
                for (Size size : state.getSelectedSizes()) {
                    Integer qty = state.getSizeQuantities().getOrDefault(size, 0);
                    productInfo.append("  • ").append(size.getLabel()).append(": ").append(qty).append(" ta\n");
                }
                
                // Save product
                botProductService.confirmAndSaveProduct(userId);
                messageService.sendProductSavedSuccess(chatId);
                
                telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Mahsulot qo'shildi"));

            } catch (Exception e) {
                log.error("Error confirming product: {}", e.getMessage(), e);
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Xatolik: " + e.getMessage())
                        .showAlert(true));
            }

        } else if (data.equals("cancel_product")) {
            botProductService.cancelProductCreation(userId);
            messageService.sendProductCreationCancelled(chatId);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Bekor qilindi"));
        }
    }

    private String buildCategoryInfo(CategoryCreationState state) {
        StringBuilder info = new StringBuilder();
        info.append("<b>Nomi:</b> ").append(state.getName()).append("\n");
        if (state.getSpotlightName() != null) {
            info.append("<b>Spotlight nomi:</b> ").append(state.getSpotlightName()).append("\n");
        }
        return info.toString();
    }
}
