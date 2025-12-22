package org.exp.primeapp.botuser.config;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botuser.handle.CallbackHandler;
import org.exp.primeapp.botuser.handle.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class BotRunner implements CommandLineRunner {

    private final TelegramBot userBot;
    private final ExecutorService executorService;
    private final MessageHandler messageHandler;
    private final CallbackHandler callbackHandler;

    @Autowired(required = false)
    public BotRunner(@org.springframework.beans.factory.annotation.Qualifier("userBot") TelegramBot userBot,
                     ExecutorService executorService, 
                     MessageHandler messageHandler, 
                     CallbackHandler callbackHandler) {
        this.userBot = userBot;
        this.executorService = executorService;
        this.messageHandler = messageHandler;
        this.callbackHandler = callbackHandler;
    }

    @PostConstruct
    public void deleteWebhookIfExists() {
        if (userBot != null) {
            BaseResponse response = userBot.execute(new DeleteWebhook());
            if (response.isOk()) log.info("✅ User bot webhook deleted successfully.");
            else log.error("❌ Failed to delete user bot webhook: {}", response.description());
        }
    }

    @Override
    public void run(String... args) {
        if (userBot == null) {
            log.warn("⚠️ User bot is disabled. Skipping user bot listener startup.");
            return;
        }

        userBot.setUpdatesListener(updates -> {
            log.debug("Received {} updates from User Bot", updates.size());
            for (Update update : updates) {
                executorService.execute(() -> {
                    try {
                        log.debug("Processing update: updateId={}, message={}, callbackQuery={}", 
                                update.updateId(), 
                                update.message() != null ? "present" : "null",
                                update.callbackQuery() != null ? "present" : "null");
                        
                        if (update.message() != null) {
                            messageHandler.accept(update.message());
                        } else if (update.callbackQuery() != null) {
                            callbackHandler.accept(update.callbackQuery());
                        } else {
                            log.warn("Unknown update type: updateId={}", update.updateId());
                        }
                    } catch (Exception e) {
                        log.error("Error processing update: updateId={}", update.updateId(), e);
                    }
                });
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        log.info("🤖 User Bot listener started successfully!");
    }
}
