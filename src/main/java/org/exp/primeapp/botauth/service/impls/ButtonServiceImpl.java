package org.exp.primeapp.botauth.service.impls;

import com.pengrad.telegrambot.model.request.*;
import org.exp.primeapp.botauth.service.interfaces.ButtonService;
import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.enums.Size;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ButtonServiceImpl implements ButtonService {

    @Override
    public Keyboard sendShareContactBtn() {
        return new ReplyKeyboardMarkup(
                new KeyboardButton("Kontaktni ulashish").requestContact(true)
        ).resizeKeyboard(true);
    }

    @Override
    public InlineKeyboardMarkup sendRenewCodeBtn() {
        return new InlineKeyboardMarkup(new InlineKeyboardButton("🔄Yangilash").callbackData("renew_code"));
    }

    @Override
    public InlineKeyboardMarkup createCategoryButtons(List<Category> categories) {
        List<InlineKeyboardButton[]> buttons = new ArrayList<>();
        
        for (Category category : categories) {
            InlineKeyboardButton button = new InlineKeyboardButton(category.getName())
                    .callbackData("select_category_" + category.getId());
            buttons.add(new InlineKeyboardButton[]{button});
        }
        
        return new InlineKeyboardMarkup(buttons.toArray(new InlineKeyboardButton[0][]));
    }

    @Override
    public InlineKeyboardMarkup createSizeButtons(List<Size> allSizes, List<Size> selectedSizes) {
        List<InlineKeyboardButton[]> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        
        for (int i = 0; i < allSizes.size(); i++) {
            Size size = allSizes.get(i);
            String prefix = selectedSizes.contains(size) ? "✅ " : "";
            InlineKeyboardButton button = new InlineKeyboardButton(prefix + size.getLabel())
                    .callbackData("toggle_size_" + size.name());
            
            row.add(button);
            
            // 3 ta button qator bo'lganda yangi qator yaratamiz
            if (row.size() == 3 || i == allSizes.size() - 1) {
                buttons.add(row.toArray(new InlineKeyboardButton[0]));
                row.clear();
            }
        }
        
        // Davom etish tugmasi
        if (!selectedSizes.isEmpty()) {
            buttons.add(new InlineKeyboardButton[]{
                    new InlineKeyboardButton("✅ Davom etish").callbackData("continue_sizes")
            });
        }
        
        return new InlineKeyboardMarkup(buttons.toArray(new InlineKeyboardButton[0][]));
    }

    @Override
    public InlineKeyboardMarkup createConfirmationButtons() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("✅ Qo'shish").callbackData("confirm_product"),
                        new InlineKeyboardButton("❌ Bekor qilish").callbackData("cancel_product")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createContinueOrFinishImageButtons() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("📷 Yana rasm qo'shish (max 3)").callbackData("add_more_image"),
                        new InlineKeyboardButton("✅ Davom etish").callbackData("continue_images")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createNextStepImageButton() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("➡️ Keyingi qadamga o'tish").callbackData("continue_images")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createAddProductButton() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("➕ Mahsulot qo'shish").callbackData("add_product")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createAdminMainMenuButtons() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("🛍️ Product").callbackData("admin_menu_product")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("📂 Category").callbackData("admin_menu_category")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("📦 Orders").callbackData("admin_menu_orders")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createProductMenuButtons() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("➕ Add").callbackData("add_product"),
                        new InlineKeyboardButton("✏️ Edit").callbackData("admin_product_edit")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("📥 Add Income").callbackData("admin_product_add_income")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("⬅️ Back").callbackData("admin_menu_back")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createCategoryMenuButtons() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("➕ Add").callbackData("admin_category_add"),
                        new InlineKeyboardButton("✏️ Edit").callbackData("admin_category_edit")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("⬅️ Back").callbackData("admin_menu_back")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createSpotlightNameButtons() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("👕 Tepa kiyimlar").callbackData("spotlight_tepa_kiyimlar")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("👖 Shimlar").callbackData("spotlight_shimlar")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("👟 Oyoq kiyimlar").callbackData("spotlight_oyoq_kiyimlar")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("💼 Aksessuarlar").callbackData("spotlight_aksessuarlar")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createCategoryConfirmationButtons() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("✅ Qo'shish").callbackData("confirm_category"),
                        new InlineKeyboardButton("❌ Bekor qilish").callbackData("cancel_category")
                }
        );
    }
}
