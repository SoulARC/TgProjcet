package tel.bot.streampostsbot.service;

import org.telegram.telegrambots.meta.api.objects.Update;
import tel.bot.streampostsbot.entity.AppUser;
import tel.bot.streampostsbot.entity.Channel;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.WorkingGroup;

import java.util.List;

public interface BotManagerService {
    AppUser findOrSaveAppUser(Update update);

    String removeGroup(AppUser appUser, WorkingGroup workingGroup);

    String removeChannel(Channel channel, WorkingGroup workingGroup);

    String removeHashtag(Hashtag hashtag, Channel channel);

    void addWorkingGroup(String title, Long channelId, AppUser appUser, List<WorkingGroup> listGroup);

    void addChannel(String title, Long channelId, WorkingGroup workingGroup, List<Channel> listChanel);

    String addHashtag(String messageText, WorkingGroup workingGroup, Channel channel);
}
