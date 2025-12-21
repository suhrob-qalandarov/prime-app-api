package org.exp.primeapp.botauth.service.impls;

import com.pengrad.telegrambot.model.request.*;
import org.exp.primeapp.botauth.models.ProductColor;
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
    public Keyboard createAdminMainReplyKeyboard() {
        return new ReplyKeyboardMarkup(
                new KeyboardButton[]{
                        new KeyboardButton("📊 Dashboard"),
                        new KeyboardButton("📦 Buyurtmalar")
                },
                new KeyboardButton[]{
                        new KeyboardButton("🛍️ Mahsulotlar"),
                        new KeyboardButton("📂 Kategoriyalar")
                },
                new KeyboardButton[]{
                        new KeyboardButton("👥 Foydalanuvchilar")
                }
        ).resizeKeyboard(true);
    }

    @Override
    public Keyboard createAdminCancelReplyKeyboard() {
        return new ReplyKeyboardMarkup(
                new KeyboardButton("❌ Bekor qilish")
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
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createCategoryMenuButtons() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("➕ Add").callbackData("admin_category_add"),
                        new InlineKeyboardButton("✏️ Edit").callbackData("admin_category_edit")
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
    public InlineKeyboardMarkup createSpotlightNameButtonsWithBack() {
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
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("⬅️ 5-chi qadamga qaytish").callbackData("back_to_additional_images")
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

    @Override
    public InlineKeyboardMarkup createSetAdminButton() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("👨‍💼 Set Admin").callbackData("set_admin_search")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createUserRoleButtons(boolean canSetAdmin, boolean canSetSuperAdmin, Long userId) {
        List<InlineKeyboardButton[]> buttons = new ArrayList<>();
        
        if (canSetAdmin) {
            buttons.add(new InlineKeyboardButton[]{
                    new InlineKeyboardButton("👨‍💼 Set Admin").callbackData("set_admin_" + userId)
            });
        }
        
        if (canSetSuperAdmin) {
            buttons.add(new InlineKeyboardButton[]{
                    new InlineKeyboardButton("👑 Set Super Admin").callbackData("set_super_admin_" + userId)
            });
        }
        
        return new InlineKeyboardMarkup(buttons.toArray(new InlineKeyboardButton[0][]));
    }

    @Override
    public InlineKeyboardMarkup createNextStepButton() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("➡️ Keyingi qadam").callbackData("skip_brand")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createSkipAdditionalImagesButton() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("➡️ Keyingi qadam").callbackData("skip_additional_images")
                }
        );
    }

    @Override
    public InlineKeyboardMarkup createColorButtons() {
        List<InlineKeyboardButton[]> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        
        ProductColor[] colors = ProductColor.getAllColors();
        for (int i = 0; i < colors.length; i++) {
            ProductColor color = colors[i];
            // Emoji bilan rang ko'rsatish
            String colorName = color.getName();
            String colorHex = color.getHex();
            String emoji = getColorEmoji(colorName);
            InlineKeyboardButton button = new InlineKeyboardButton(emoji + " " + colorName)
                    .callbackData("select_color_" + colorName + "_" + colorHex);
            
            row.add(button);
            
            // 3 ta button qator bo'lganda yangi qator yaratamiz
            if (row.size() == 3 || i == colors.length - 1) {
                buttons.add(row.toArray(new InlineKeyboardButton[0]));
                row.clear();
            }
        }
        
        // Add "Keyingi qadam" button to skip color selection
        buttons.add(new InlineKeyboardButton[]{
                new InlineKeyboardButton("➡️ Keyingi qadam").callbackData("skip_color")
        });
        
        return new InlineKeyboardMarkup(buttons.toArray(new InlineKeyboardButton[0][]));
    }
    
    public InlineKeyboardMarkup addBackButton(InlineKeyboardMarkup original, String step) {
        if (original == null || original.inlineKeyboard() == null) {
            return createBackButton(step);
        }
        
        List<InlineKeyboardButton[]> buttons = new ArrayList<>();
        // Add all existing buttons
        for (InlineKeyboardButton[] row : original.inlineKeyboard()) {
            if (row != null && row.length > 0) {
                buttons.add(row);
            }
        }
        // Add back button as a new row
        buttons.add(new InlineKeyboardButton[]{
                new InlineKeyboardButton("⬅️ Orqaga").callbackData("back_to_" + step)
        });
        
        return new InlineKeyboardMarkup(buttons.toArray(new InlineKeyboardButton[0][]));
    }
    
    private String getColorEmoji(String colorName) {
        return switch (colorName) {
            case "Qora" -> "⚫";
            case "Oq" -> "⚪";
            case "Qizil" -> "🔴";
            case "Ko'k" -> "🔵";
            case "Yashil" -> "🟢";
            case "Sariq" -> "🟡";
            case "Qizg'ish" -> "🩷";
            case "Kulrang" -> "⚪";
            case "Jigarrang" -> "🟤";
            default -> "🎨";
        };
    }

    @Override
    public InlineKeyboardMarkup createBackButton(String step) {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("⬅️ Orqaga").callbackData("back_to_" + step)
                }
        );
    }
}
