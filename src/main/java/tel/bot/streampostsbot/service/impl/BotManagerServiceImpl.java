package tel.bot.streampostsbot.service.impl;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import tel.bot.streampostsbot.dao.AppUserDAO;
import tel.bot.streampostsbot.dao.ChannelDAO;
import tel.bot.streampostsbot.dao.HashtagDAO;
import tel.bot.streampostsbot.dao.WorkingGroupDAO;
import tel.bot.streampostsbot.entity.AppUser;
import tel.bot.streampostsbot.entity.Channel;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.WorkingGroup;
import tel.bot.streampostsbot.service.BotManagerService;

import java.util.List;
import java.util.Optional;

@Service
public class BotManagerServiceImpl implements BotManagerService {
    private final AppUserDAO appUserDAO;
    private final ChannelDAO channelDAO;
    private final HashtagDAO hashtagDAO;
    private final WorkingGroupDAO workingGroupDAO;

    public BotManagerServiceImpl(
            AppUserDAO appUserDAO,
            ChannelDAO channelDAO,
            HashtagDAO hashtagDAO,
            WorkingGroupDAO workingGroupDAO
    ) {
        this.appUserDAO = appUserDAO;
        this.channelDAO = channelDAO;
        this.hashtagDAO = hashtagDAO;
        this.workingGroupDAO = workingGroupDAO;
    }

    @Override
    public AppUser findOrSaveAppUser(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        User telegramUser = update.getMessage().getFrom();
        AppUser persistentAppUser = appUserDAO.findAppUserByTelegramUserId(userId);
        if (Optional.ofNullable(persistentAppUser).isEmpty()) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(userId)
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return persistentAppUser;
    }
    //TODO Без норм квері працювать не буде
    @Override
    public String removeGroup(AppUser appUser, WorkingGroup workingGroup) {
        appUser.getWorkingGroups().remove(workingGroup);
        appUserDAO.save(appUser);
        workingGroupDAO.delete(workingGroup);
        return String.format("\"%s\" group has been deleted", workingGroup.getNameChannel());
    }
    //TODO А це буде але так собі
    @Override
    public String removeChannel(Channel channel, WorkingGroup workingGroup) {
        WorkingGroup oldWorkingGroup = channel.getWorkingGroups().get(0);
        workingGroup.getChannels().remove(channel);
        workingGroupDAO.save(workingGroup);
        channel.getWorkingGroups().remove(oldWorkingGroup);
        channelDAO.save(channel);
        return String.format("\"%s\" channel has been deleted", channel.getChannelName());
    }

    @Override
    public String removeHashtag(Hashtag hashtag, Channel channel) {
        channel.getHashtags().remove(hashtag);
        channelDAO.save(channel);
        hashtagDAO.delete(hashtag);
        return String.format("Hashtag %s has ben deleted", hashtag.getHashtagName());
    }
    @Override
    public void addWorkingGroup(String title, Long channelId, AppUser appUser, List<WorkingGroup> listGroup) {
        WorkingGroup newGroup = WorkingGroup.builder()
                .channelId(channelId)
                .nameChannel(title)
                .appUser(appUser)
                .build();
        workingGroupDAO.save(newGroup);
        listGroup.add(newGroup);
        appUser.setWorkingGroups(listGroup);
        appUserDAO.save(appUser);
    }
    @Override
    public void addChannel(String title, Long channelId, WorkingGroup workingGroup, List<Channel> listChanel) {
        Channel channel = channelDAO.findChannelByChannelId(channelId);
        if (Optional.ofNullable(channel).isPresent()) {
            channel.getWorkingGroups().add(workingGroup);
        }
        else {
            channel = Channel.builder()
                    .channelId(channelId)
                    .channelName(title)
                    .workingGroups(List.of(workingGroup))
                    .build();
        }
        channelDAO.save(channel);
        listChanel.add(channel);
        workingGroup.setChannels(listChanel);
        workingGroupDAO.save(workingGroup);
    }

    @Override
    public String addHashtag(String messageText, WorkingGroup workingGroup, Channel channel) {
        Hashtag newHashtag = Hashtag.builder()
                .hashtagName(messageText)
                .workingGroup(workingGroup)
                .build();
        hashtagDAO.save(newHashtag);
        channel.getHashtags().add(newHashtag);
        channelDAO.save(channel);

        return String.format("Hashtag %s added ", messageText);
    }
}
