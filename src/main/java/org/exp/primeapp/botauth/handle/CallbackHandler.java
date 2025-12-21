package org.exp.primeapp.botauth.handle;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botauth.models.CategoryCreationState;
import org.exp.primeapp.botauth.models.ProductCreationState;
import org.exp.primeapp.botauth.service.interfaces.BotCategoryService;
import org.exp.primeapp.botauth.service.interfaces.BotProductService;
import org.exp.primeapp.botauth.service.interfaces.ButtonService;
import org.exp.primeapp.botauth.service.interfaces.MessageService;
import org.exp.primeapp.botauth.service.interfaces.UserService;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.User;
import org.exp.primeapp.models.enums.Size;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackHandler implements Consumer<CallbackQuery> {

    private final UserService userService;
    private final MessageService messageService;
    private final BotProductService botProductService;
    private final BotCategoryService botCategoryService;
    private final ButtonService buttonService;
    private final TelegramBot telegramBot;

    @Override
    public void accept(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        String callbackId = callbackQuery.id();
        User user = userService.getOrCreateUser(callbackQuery.from());
        Long userId = user.getId();
        Long chatId = user.getTelegramId();

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

        if (data.equals("renew_code")) {
            if (user.getVerifyCodeExpiration().isAfter(LocalDateTime.now())) {
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Eski kodingiz hali ham kuchda ☝️")
                        .showAlert(true));
                return;
            }
            messageService.renewCode(user);
            return;
        }

        if (data.equals("add_product")) {
            botProductService.startProductCreation(userId);
            messageService.sendProductCreationStart(chatId);
            messageService.sendProductNamePrompt(chatId);
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Mahsulot qo'shish boshlandi"));
            return;
        }

        // Handle product creation callbacks
        ProductCreationState state = botProductService.getProductCreationState(userId);
        if (state == null && !data.equals("renew_code")) {
            // Don't show error for admin menu callbacks
            if (!data.startsWith("admin_")) {
                telegramBot.execute(new AnswerCallbackQuery(callbackId)
                        .text("Mahsulot qo'shish jarayoni topilmadi. /add_product bilan boshlang.")
                        .showAlert(true));
            }
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
                state.setCurrentStep(ProductCreationState.Step.WAITING_IMAGES);
                int currentCount = state.getAttachmentUrls() != null ? state.getAttachmentUrls().size() : 0;
                messageService.sendProductImagePrompt(chatId, currentCount);
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
