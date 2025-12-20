package org.exp.primeapp.botauth.config;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botauth.handle.CallbackHandler;
import org.exp.primeapp.botauth.handle.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class BotRunner implements CommandLineRunner {

    private final TelegramBot bot;
    private final ExecutorService executorService;
    private final MessageHandler messageHandler;
    private final CallbackHandler callbackHandler;

    @Autowired(required = false)
    public BotRunner(TelegramBot bot, ExecutorService executorService, 
                     MessageHandler messageHandler, CallbackHandler callbackHandler) {
        this.bot = bot;
        this.executorService = executorService;
        this.messageHandler = messageHandler;
        this.callbackHandler = callbackHandler;
    }

    @PostConstruct
    public void deleteWebhookIfExists() {
        if (bot == null) {
            log.warn("⚠️ Telegram bot is disabled. Skipping webhook deletion.");
            return;
        }

        BaseResponse response = bot.execute(new DeleteWebhook());
        if (response.isOk()) log.info("✅ Webhook deleted successfully.");
        else log.error("❌ Failed to delete webhook: {}", response.description());
    }

    @Override
    public void run(String... args) {
        if (bot == null) {
            log.warn("⚠️ Telegram bot is disabled. Skipping bot listener startup.");
            return;
        }

        bot.setUpdatesListener(updates -> {
            log.debug("Received {} updates from Telegram", updates.size());
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

        log.info("🤖 Telegram bot listener started successfully!");
    }
}
