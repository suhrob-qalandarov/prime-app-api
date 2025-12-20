package org.exp.primeapp.botauth.handle;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botauth.models.ProductCreationState;
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
    private final ButtonService buttonService;
    private final TelegramBot telegramBot;

    @Override
    public void accept(CallbackQuery callbackQuery) {
        String data = callbackQuery.data();
        String callbackId = callbackQuery.id();
        User user = userService.getOrCreateUser(callbackQuery.from());
        Long userId = user.getId();
        Long chatId = user.getTelegramId();

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
            telegramBot.execute(new AnswerCallbackQuery(callbackId)
                    .text("Mahsulot qo'shish jarayoni topilmadi. /add_product bilan boshlang.")
                    .showAlert(true));
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
            
            state.setCurrentStep(ProductCreationState.Step.WAITING_CATEGORY);
            List<Category> categories = botProductService.getAllCategories();
            messageService.sendCategorySelection(chatId);
            telegramBot.execute(new SendMessage(chatId, "📂 Kategoriyani tanlang:")
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(buttonService.createCategoryButtons(categories))
            );
            
            telegramBot.execute(new AnswerCallbackQuery(callbackId).text("Rasmlar qabul qilindi"));

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
}
