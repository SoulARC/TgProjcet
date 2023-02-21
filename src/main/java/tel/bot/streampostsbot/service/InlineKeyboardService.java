package tel.bot.streampostsbot.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import tel.bot.streampostsbot.entity.AppUser;
import tel.bot.streampostsbot.entity.Channel;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.WorkingGroup;

public interface InlineKeyboardService {
    SendMessage inlineKeyboardChannelsList(Long chatId, WorkingGroup workingGroup);

    SendMessage inlineKeyboardChannel(Long chatId, Channel channel);

    SendMessage inlineKeyboardMyGroup(Long chatId, AppUser appUser);

    SendMessage inlineKeyboardHashtag(Long chatId, Hashtag hashtag);
}
