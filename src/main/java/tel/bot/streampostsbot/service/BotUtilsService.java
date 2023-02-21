package tel.bot.streampostsbot.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.Message;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.MainChannel;

import java.util.List;

public interface BotUtilsService {
    boolean captionValidator(Message channelPost, MainChannel group, List<Hashtag> hashtagList);

    boolean captionMediaGroupValidator(String caption, MainChannel group, List<Hashtag> hashtagList);

    boolean hashtagIsPresent(String hashtag, List<Hashtag> hashtagList);

    SendMediaGroup getMediaGroup(List<Message> mediaGroupList);
}
