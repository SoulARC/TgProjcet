package tel.bot.streampostsbot.service;

import org.telegram.telegrambots.meta.api.objects.Update;
import tel.bot.streampostsbot.entity.AppUser;
import tel.bot.streampostsbot.entity.Channel;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.MainChannel;

import java.util.List;

public interface BotManagerService {
    AppUser findOrSaveAppUser(Update update);

    String removeChannel(Channel channel, MainChannel mainChannel);

    String removeHashtag(Hashtag hashtag, Channel channel);

    void addWorkingGroup(String title, Long channelId, AppUser appUser, List<MainChannel> listGroup);

    void addChannel(String title, Long channelId, MainChannel mainChannel, List<Channel> listChanel);

    String addHashtag(String messageText, MainChannel mainChannel, Channel channel);
}
