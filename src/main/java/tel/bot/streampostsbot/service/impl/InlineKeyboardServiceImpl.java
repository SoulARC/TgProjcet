package tel.bot.streampostsbot.service.impl;

import static tel.bot.streampostsbot.service.enums.ChannelCommands.ADD_HASHTAG;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.BACK_TO_CHANNELS_LIST;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.REMOVE_CHANNEL;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.REMOVE_HASHTAG;
import static tel.bot.streampostsbot.service.enums.Flags.FLAG_CHANNEL_LIST;
import static tel.bot.streampostsbot.service.enums.Flags.FLAG_GROUP_LIST;
import static tel.bot.streampostsbot.service.enums.Flags.FLAG_HASHTAG;
import static tel.bot.streampostsbot.service.enums.WorkGroupCommands.ADD_NEW_CHANNEL;
import static tel.bot.streampostsbot.service.enums.WorkGroupCommands.ADD_WORKING_GROUP;
import static tel.bot.streampostsbot.service.enums.WorkGroupCommands.BACK_TO_GROUPS_LIST;
import static tel.bot.streampostsbot.service.enums.WorkGroupCommands.DELETE_MAIN_CHANNEL;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import tel.bot.streampostsbot.entity.AppUser;
import tel.bot.streampostsbot.entity.Channel;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.MainChannel;
import tel.bot.streampostsbot.service.InlineKeyboardService;

import java.util.ArrayList;
import java.util.List;

@Service
public class InlineKeyboardServiceImpl implements InlineKeyboardService {
    @Override
    public SendMessage inlineKeyboardChannelsList(Long chatId, MainChannel mainChannel) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(mainChannel.getNameChannel()
                + " group");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<Channel> listGroup = mainChannel.getChannels();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        for (int i = 0; i < listGroup.size(); ) {
            StringBuilder strBuilder = new StringBuilder();
            Channel channel = listGroup.get(i);
            strBuilder.append(++i)
                    .append(") Title (")
                    .append(channel.getChannelName())
                    .append(") id(")
                    .append(channel.getChannelId())
                    .append(")");
            rowsInLine.add(inlineButtonBuilder(
                    strBuilder.toString(),
                    FLAG_CHANNEL_LIST + channel.getId().toString()
            ));
        }
        rowsInLine.add(inlineButtonBuilder("Add new channel", ADD_NEW_CHANNEL.toString()));
        rowsInLine.add(inlineButtonBuilder("↪️Back", BACK_TO_GROUPS_LIST.toString()));
        rowsInLine.add(inlineButtonBuilder("❌ Delete group", DELETE_MAIN_CHANNEL.toString()));
        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);
        return sendMessage;
    }

    @Override
    public SendMessage inlineKeyboardChannel(Long chatId, Channel channel) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(channel.getChannelName()
                + " group");
        sendMessage.setChatId(chatId);
        List<Hashtag> hashtagList = channel.getHashtags();
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        for (int i = 0; i < hashtagList.size(); ) {
            StringBuilder strBuilder = new StringBuilder();
            Hashtag hashtag = hashtagList.get(i);
            strBuilder.append(++i)
                    .append(hashtag.getHashtagName());
            rowsInLine.add(inlineButtonBuilder(
                    strBuilder.toString(),
                    FLAG_HASHTAG + hashtag.getId().toString()
            ));
        }
        rowsInLine.add(inlineButtonBuilder("Add target hashtag", ADD_HASHTAG.toString()));
        rowsInLine.add(inlineButtonBuilder("↪️Back", BACK_TO_CHANNELS_LIST.toString()));
        rowsInLine.add(inlineButtonBuilder("❌ Remove channel", REMOVE_CHANNEL.toString()));
        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);
        return sendMessage;
    }

    @Override
    public SendMessage inlineKeyboardMyGroup(Long chatId, AppUser appUser) {
        SendMessage sendMessage = new SendMessage();
        List<MainChannel> listGroup = appUser.getMMainChannels();
        sendMessage.setChatId(chatId);
        sendMessage.setText("My list working group");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        for (int i = 0; i < listGroup.size(); ) {
            StringBuilder strBuilder = new StringBuilder();
            MainChannel mainChannel = listGroup.get(i);
            strBuilder.append(++i)
                    .append(") Title (")
                    .append(mainChannel.getNameChannel())
                    .append(") id(")
                    .append(mainChannel.getChannelId())
                    .append(")");
            rowsInLine.add(inlineButtonBuilder(
                    strBuilder.toString(),
                    FLAG_GROUP_LIST + mainChannel.getId().toString()
            ));
        }
        rowsInLine.add(inlineButtonBuilder("Add group", ADD_WORKING_GROUP.toString()));
        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);
        return sendMessage;
    }

    @Override
    public SendMessage inlineKeyboardHashtag(Long chatId, Hashtag hashtag) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(hashtag.getHashtagName()
                + " hashtag");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        rowsInLine.add(inlineButtonBuilder("↪️Back", BACK_TO_CHANNELS_LIST.toString()));
        rowsInLine.add(inlineButtonBuilder("❌ Remove target hashtag", REMOVE_HASHTAG.toString()));
        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);
        return sendMessage;
    }

    private List<InlineKeyboardButton> inlineButtonBuilder(String text, String callbackData) {
        List<InlineKeyboardButton> listButton = new ArrayList<>();
        InlineKeyboardButton newInlineButton = new InlineKeyboardButton();
        newInlineButton.setText(text);
        newInlineButton.setCallbackData(callbackData);
        listButton.add(newInlineButton);
        return listButton;
    }
}
