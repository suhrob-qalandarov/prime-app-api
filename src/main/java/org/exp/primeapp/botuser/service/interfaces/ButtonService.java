package org.exp.primeapp.botuser.service.interfaces;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;

public interface ButtonService {
    Keyboard sendShareContactBtn();

    InlineKeyboardMarkup sendRenewCodeBtn();
}
