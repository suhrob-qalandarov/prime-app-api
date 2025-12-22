package org.exp.primeapp.botuser.config;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.GetMe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BotConfig {

    @Value("${telegram.bot.user.token}")
    private String userBotToken;

    @Bean(name = "userBot")
    public TelegramBot userBot() {
        return createBot(userBotToken, "User Bot");
    }

    private TelegramBot createBot(String token, String botName) {
        try {
            if (token == null || token.isBlank()) {
                log.warn("⚠️ {} token is empty in application.properties. Bot will be disabled.", botName);
                return null;
            }
            
            log.info("✅ {} token found. Initializing bot... (token length: {})", botName, token.length());
            TelegramBot bot = new TelegramBot(token);
            
            // Test bot connection
            try {
                var me = bot.execute(new GetMe());
                if (me.isOk()) {
                    log.info("✅ {} initialized successfully! Bot username: @{}", botName, me.user().username());
                } else {
                    log.error("❌ Failed to get {} info. Error: {}", botName, me.description());
                }
            } catch (Exception e) {
                log.error("❌ Error testing {} connection: {}", botName, e.getMessage());
            }
            
            return bot;
        } catch (Exception e) {
            log.error("❌ Error initializing {}: {}", botName, e.getMessage(), e);
            return null;
        }
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(30);
    }
}