package tel.bot.streampostsbot.service.impl;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import tel.bot.streampostsbot.dao.HashtagDAO;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.MainChannel;
import tel.bot.streampostsbot.service.BotUtilsService;

import java.util.ArrayList;
import java.util.List;

@Service
public class BotUtilsServiceImpl implements BotUtilsService {
    private final HashtagDAO hashtagDAO;

    public BotUtilsServiceImpl(HashtagDAO hashtagDAO) {
        this.hashtagDAO = hashtagDAO;
    }

    @Override
    public boolean captionValidator(Message channelPost, MainChannel group, List<Hashtag> hashtagList) {
        List<String> listHashtags =
                hashtagDAO.getHashtagsByMainChannel(group).stream()
                        .filter(hashtagList::contains)
                        .map(Hashtag::getHashtagName)
                        .toList();

        return (!channelPost.hasEntities()
                || channelPost.getEntities().stream()
                .noneMatch(e -> listHashtags.contains(e.getText())))
                && (channelPost.getCaptionEntities() == null
                || channelPost.getCaptionEntities().stream()
                .noneMatch(e -> listHashtags.contains(e.getText())));
    }
    @Override
    public boolean captionMediaGroupValidator(String caption, MainChannel group, List<Hashtag> hashtagList) {
        List<String> listHashtags =
                hashtagDAO.getHashtagsByMainChannel(group).stream()
                        .filter(hashtagList::contains)
                        .map(Hashtag::getHashtagName)
                        .toList();

        return (caption == null
                || listHashtags.stream()
                .noneMatch(caption::contains));
    }

    @Override
    public boolean hashtagIsPresent(String hashtag, List<Hashtag> hashtagList) {
        List<String> listHashtag = hashtagList.stream()
                .map(Hashtag::getHashtagName)
                .toList();
        return listHashtag.contains(hashtag);
    }

    @Override
    public SendMediaGroup getMediaGroup(List<Message> mediaGroupList) {
        SendMediaGroup mediaGroup = new SendMediaGroup();
        List<InputMedia> inputMedias = new ArrayList<>();
        for (Message channelPost : mediaGroupList) {
            String caption = channelPost.getCaption();
            if (channelPost.getPhoto() != null) {
                inputMedias.add(InputMediaPhoto.builder()
                        .caption(caption)
                        .media(channelPost.getPhoto().get(
                                channelPost.getPhoto().size() - 1).getFileId())
                        .build());
                mediaGroup.setMedias(inputMedias);
            }
            else if (channelPost.getVideo() != null) {
                inputMedias.add(InputMediaVideo.builder()
                        .caption(caption)
                        .media(channelPost.getVideo().getFileId())
                        .build());
                mediaGroup.setMedias(inputMedias);
            }
            else if (channelPost.getDocument() != null) {
                inputMedias.add(InputMediaDocument.builder()
                        .caption(caption)
                        .media(channelPost.getDocument().getFileId())
                        .build());
                mediaGroup.setMedias(inputMedias);
            }
        }
        return mediaGroup;
    }

}
