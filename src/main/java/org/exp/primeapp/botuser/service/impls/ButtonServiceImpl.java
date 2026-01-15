package org.exp.primeapp.botuser.service.impls;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import org.exp.primeapp.botuser.service.interfaces.ButtonService;
import org.springframework.stereotype.Service;

@Service
public class ButtonServiceImpl implements ButtonService {

    @Override
    public Keyboard sendShareContactBtn() {
        return new ReplyKeyboardMarkup(
                new KeyboardButton("Kontaktni ulashish")
                        .requestContact(true)
        ).resizeKeyboard(true);
    }

    @Override
    public InlineKeyboardMarkup sendRenewCodeBtn() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("🔄Yangilash")
                        .callbackData("renew_code")
        );
    }
}
