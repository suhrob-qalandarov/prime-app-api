package org.exp.primeapp.botuser.handle;

import com.pengrad.telegrambot.model.CallbackQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackHandler implements Consumer<CallbackQuery> {

    @Override
    public void accept(CallbackQuery callbackQuery) {
        // User bot currently doesn't handle callbacks
        log.debug("User bot received callback query, but callbacks are not handled");
    }
}
