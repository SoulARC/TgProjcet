package tel.bot.streampostsbot.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface WorkingGroupService {
    SendMessage inputRequest(Update update);
}
