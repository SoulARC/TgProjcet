package tel.bot.streampostsbot.service.impl;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import tel.bot.streampostsbot.dao.AppUserDAO;
import tel.bot.streampostsbot.dao.ChannelDAO;
import tel.bot.streampostsbot.dao.HashtagDAO;
import tel.bot.streampostsbot.dao.MainChannelDAO;
import tel.bot.streampostsbot.entity.AppUser;
import tel.bot.streampostsbot.entity.Channel;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.MainChannel;
import tel.bot.streampostsbot.service.BotManagerService;

import java.util.List;
import java.util.Optional;

@Service
public class BotManagerServiceImpl implements BotManagerService {
    private final AppUserDAO appUserDAO;
    private final ChannelDAO channelDAO;
    private final HashtagDAO hashtagDAO;
    private final MainChannelDAO mMainChannelDAO;

    public BotManagerServiceImpl(
            AppUserDAO appUserDAO,
            ChannelDAO channelDAO,
            HashtagDAO hashtagDAO,
            MainChannelDAO mainChannelDAO
    ) {
        this.appUserDAO = appUserDAO;
        this.channelDAO = channelDAO;
        this.hashtagDAO = hashtagDAO;
        this.mMainChannelDAO = mainChannelDAO;
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
    public String removeGroup(AppUser appUser, MainChannel mainChannel) {
        appUser.getMMainChannels().remove(mainChannel);
        appUserDAO.save(appUser);
        mMainChannelDAO.delete(mainChannel);
        return String.format("\"%s\" group has been deleted", mainChannel.getNameChannel());
    }
    //TODO А це буде але так собі
    @Override
    public String removeChannel(Channel channel, MainChannel mainChannel) {
        MainChannel oldMainChahnnel = channel.getMainChannels().get(0);
        mainChannel.getChannels().remove(channel);
        mMainChannelDAO.save(mainChannel);
        channel.getMainChannels().remove(oldMainChahnnel);
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
    public void addWorkingGroup(String title, Long channelId, AppUser appUser, List<MainChannel> listGroup) {
        MainChannel newGroup = MainChannel.builder()
                .channelId(channelId)
                .nameChannel(title)
                .appUser(appUser)
                .build();
        mMainChannelDAO.save(newGroup);
        listGroup.add(newGroup);
        appUser.setMMainChannels(listGroup);
        appUserDAO.save(appUser);
    }
    @Override
    public void addChannel(String title, Long channelId, MainChannel mainChannel, List<Channel> listChanel) {
        Channel channel = channelDAO.findChannelByChannelId(channelId);
        if (Optional.ofNullable(channel).isPresent()) {
            channel.getMainChannels().add(mainChannel);
        }
        else {
            channel = Channel.builder()
                    .channelId(channelId)
                    .channelName(title)
                    .mainChannels(List.of(mainChannel))
                    .build();
        }
        channelDAO.save(channel);
        listChanel.add(channel);
        mainChannel.setChannels(listChanel);
        mMainChannelDAO.save(mainChannel);
    }

    @Override
    public String addHashtag(String messageText, MainChannel mainChannel, Channel channel) {
        Hashtag newHashtag = Hashtag.builder()
                .hashtagName(messageText)
                .mainChannel(mainChannel)
                .build();
        hashtagDAO.save(newHashtag);
        channel.getHashtags().add(newHashtag);
        channelDAO.save(channel);

        return String.format("Hashtag %s added ", messageText);
    }
}
