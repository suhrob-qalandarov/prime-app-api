package org.exp.primeapp.botadmin.config;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.botadmin.handle.AdminCallbackHandler;
import org.exp.primeapp.botadmin.handle.AdminMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class AdminBotRunner implements CommandLineRunner {

    private final TelegramBot adminBot;
    private final ExecutorService adminExecutorService;
    private final AdminMessageHandler adminMessageHandler;
    private final AdminCallbackHandler adminCallbackHandler;

    @Autowired(required = false)
    public AdminBotRunner(@org.springframework.beans.factory.annotation.Qualifier("adminBot") TelegramBot adminBot,
                          @org.springframework.beans.factory.annotation.Qualifier("adminExecutorService") ExecutorService adminExecutorService,
                          AdminMessageHandler adminMessageHandler,
                          AdminCallbackHandler adminCallbackHandler) {
        this.adminBot = adminBot;
        this.adminExecutorService = adminExecutorService;
        this.adminMessageHandler = adminMessageHandler;
        this.adminCallbackHandler = adminCallbackHandler;
    }

    @PostConstruct
    public void deleteWebhookIfExists() {
        if (adminBot != null) {
            BaseResponse response = adminBot.execute(new DeleteWebhook());
            if (response.isOk()) log.info("✅ Admin bot webhook deleted successfully.");
            else log.error("❌ Failed to delete admin bot webhook: {}", response.description());
        }
    }

    @Override
    public void run(String... args) {
        if (adminBot == null) {
            log.warn("⚠️ Admin bot is disabled. Skipping admin bot listener startup.");
            return;
        }

        adminBot.setUpdatesListener(updates -> {
            log.debug("Received {} updates from Admin Bot", updates.size());
            for (Update update : updates) {
                adminExecutorService.execute(() -> {
                    try {
                        log.debug("Processing update: updateId={}, message={}, callbackQuery={}", 
                                update.updateId(), 
                                update.message() != null ? "present" : "null",
                                update.callbackQuery() != null ? "present" : "null");
                        
                        if (update.message() != null) {
                            adminMessageHandler.accept(update.message());
                        } else if (update.callbackQuery() != null) {
                            adminCallbackHandler.accept(update.callbackQuery());
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

        log.info("🤖 Admin Bot listener started successfully!");
    }
}

