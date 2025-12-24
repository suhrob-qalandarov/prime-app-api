package org.exp.primeapp.botadmin.utils;

public interface AdminBotMsgConst {
    
    // Main menu buttons
    String BTN_DASHBOARD = "📊 Dashboard";
    String BTN_ORDERS = "📦 Buyurtmalar";
    String BTN_PRODUCTS = "🛍️ Mahsulotlar";
    String BTN_CATEGORIES = "📂 Kategoriyalar";
    String BTN_USERS = "👥 Foydalanuvchilar";
    
    // Product buttons
    String BTN_NEW_PRODUCT = "➕ Yangi mahsulot";
    String BTN_EDIT_PRODUCT = "✏️ Mahsulot o'zgartirish";
    String BTN_INCOME = "📥 Income";
    String BTN_OUTCOME = "📤 Outcome";
    String BTN_MAIN_MENU = "🏠 Asosiy menyu";
    String BTN_CANCEL_PRODUCT = "❌ Yangi mahsulotni bekor qilish";
    String BTN_CANCEL = "❌ Bekor qilish";
    
    // Messages
    String MSG_WELCOME_ADMIN = "👨‍💼 <b>Xush kelibsiz, Admin!</b>\n\nQuyidagi bo'limlardan birini tanlang👇";
    String MSG_PRODUCTS_SECTION = "🛍️ <b>Mahsulotlar bo'limi</b>\n\nQuyidagi amallardan birini tanlang:";
    String MSG_CATEGORIES_SECTION = "📂 <b>Kategoriyalar bo'limi</b>\n\nQuyidagi amallardan birini tanlang:";
    String MSG_PRODUCT_CREATION_START = "🛍️ <b>Yangi mahsulot qo'shish</b>\n\nMahsulot qo'shish jarayonini boshlaymiz. Quyidagi ma'lumotlarni ketma-ket kiriting:";
    
    // Product creation steps
    String MSG_PRODUCT_NAME = "📝 <b>1/9</b> Mahsulot nomini kiriting:";
    String MSG_PRODUCT_DESCRIPTION = "📝 <b>2/9</b> Mahsulot tavsifini kiriting:";
    String MSG_PRODUCT_BRAND = "🏷️ <b>3/9</b> Brend nomini kiriting:";
    String MSG_PRODUCT_COLOR = "🎨 <b>4/9</b> Rangni tanlang:";
    String MSG_PRODUCT_MAIN_IMAGE = "📷 <b>5/9</b> Mahsulotning asosiy rasmlarini yuboring:";
    String MSG_PRODUCT_ADDITIONAL_IMAGES = "📷 <b>5/9</b> Mahsulotning qo'shimcha rasmlarini yuboring:";
    String MSG_PRODUCT_SPOTLIGHT = "📂 <b>6/9</b> Toifani tanlang:";
    String MSG_PRODUCT_CATEGORY = "📂 <b>7/9</b> Kategoriyani tanlang:";
    String MSG_PRODUCT_SIZE = "📏 <b>8/9</b> O'lchamlarni tanlang (bir nechtasini tanlash mumkin):";
    String MSG_PRODUCT_PRICE = "💰 <b>9/9</b> Mahsulot narxini kiriting (so'm):";
    
    // Callback prefixes
    String CALLBACK_PREFIX_PRODUCT = "product_";
    String CALLBACK_PREFIX_CATEGORY = "category_";
    String CALLBACK_PREFIX_ORDER = "order_";
    String CALLBACK_PREFIX_USER = "user_";
    
    // Common callbacks
    String CALLBACK_ADD_PRODUCT = "add_product";
    String CALLBACK_SKIP_BRAND = "skip_brand";
    String CALLBACK_SKIP_COLOR = "skip_color";
    String CALLBACK_BACK_TO = "back_to_";
    String CALLBACK_CONFIRM_PRODUCT = "confirm_product";
    String CALLBACK_CANCEL_PRODUCT = "cancel_product";
}

