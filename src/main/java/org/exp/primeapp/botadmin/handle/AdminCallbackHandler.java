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
    private final org.exp.primeapp.botadmin.service.impls.ProductCallbackHandler productCallbackHandler;

    public AdminCallbackHandler(UserService userService,
                                 AdminMessageService messageService,
                                 BotProductService botProductService,
                                 BotCategoryService botCategoryService,
                                 BotUserService botUserService,
                                 AdminButtonService buttonService,
                                 @Qualifier("adminBot") TelegramBot telegramBot,
                                 @Qualifier("userBot") TelegramBot userBot,
                                 org.exp.primeapp.botadmin.service.impls.ProductCallbackHandler productCallbackHandler) {
        this.userService = userService;
        this.messageService = messageService;
        this.botProductService = botProductService;
        this.botCategoryService = botCategoryService;
        this.botUserService = botUserService;
        this.buttonService = buttonService;
        this.telegramBot = telegramBot;
        this.userBot = userBot;
        this.productCallbackHandler = productCallbackHandler;
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
                
                // Edit toifa message to remove buttons
                com.pengrad.telegrambot.model.Message callbackMessage = callbackQuery.message();
                Integer messageId = callbackMessage != null ? callbackMessage.messageId() : null;
                if (messageId != null) {
                    String toifaText = "📂 <b>6/9</b> Toifani tanlang: " + actualSpotlightName;
                    telegramBot.execute(new EditMessageText(chatId, messageId, toifaText)
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(new InlineKeyboardMarkup(new com.pengrad.telegrambot.model.request.InlineKeyboardButton[0][]))
                    );
                }
                
                // Send category selection message with buttons
                telegramBot.execute(new SendMessage(chatId, "📂 <b>7/9</b> Kategoriyani tanlang:")
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(buttonService.addBackButton(buttonService.createCategoryButtons(categories), "WAITING_SPOTLIGHT_NAME"))
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

        // Handle product callbacks
        if (productCallbackHandler.handleCallback(callbackQuery, userId, chatId)) {
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

        // Product callbacks are now handled by ProductCallbackHandler
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
