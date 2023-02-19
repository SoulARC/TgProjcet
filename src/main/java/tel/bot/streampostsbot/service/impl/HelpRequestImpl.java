package tel.bot.streampostsbot.service.impl;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class HelpRequestImpl {
    public SendMessage help(String chatId) {
        return new SendMessage(chatId, "You really need help? Sorry, but i can't help you :(");
    }
}
