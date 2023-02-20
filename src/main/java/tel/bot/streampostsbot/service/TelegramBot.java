package tel.bot.streampostsbot.service;

import static tel.bot.streampostsbot.service.enums.ChannelCommands.ADD_HASHTAG;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.BACK_TO_CHANNELS_LIST;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.REMOVE_CHANNEL;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.REMOVE_HASHTAG;
import static tel.bot.streampostsbot.service.enums.ServiceCommands.HELP;
import static tel.bot.streampostsbot.service.enums.ServiceCommands.MY_WORKING_GROUPS;
import static tel.bot.streampostsbot.service.enums.ServiceCommands.START;
import static tel.bot.streampostsbot.service.enums.WorkGroupCommands.ADD_NEW_CHANNEL;
import static tel.bot.streampostsbot.service.enums.WorkGroupCommands.ADD_WORKING_GROUP;
import static tel.bot.streampostsbot.service.enums.WorkGroupCommands.BACK_TO_GROUPS_LIST;
import static tel.bot.streampostsbot.service.enums.WorkGroupCommands.DELETE_GROUP;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tel.bot.streampostsbot.config.BotConfig;
import tel.bot.streampostsbot.dao.AppUserDAO;
import tel.bot.streampostsbot.dao.ChannelDAO;
import tel.bot.streampostsbot.dao.HashtagDAO;
import tel.bot.streampostsbot.dao.WorkingGroupDAO;
import tel.bot.streampostsbot.entity.AppUser;
import tel.bot.streampostsbot.entity.Channel;
import tel.bot.streampostsbot.entity.Hashtag;
import tel.bot.streampostsbot.entity.WorkingGroup;
import tel.bot.streampostsbot.service.impl.HelpRequestImpl;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final String ERROR_TEXT = "Error occurred: ";
    private static final String FLAG_CHANNEL_LIST = "channelList ";
    private static final String FLAG_GROUP_LIST = "groupList ";
    private static final String FLAG_HASHTAG = "hashtag ";
    private static final String HASHTAG = "^#(?=.*[^0-9])[a-zа-яёіїґ0-9]{1,29}$";
    private static final String NOT_IMPLEMENTED = "Sorry, this feature is not yet implemented :(";
    private boolean FLAG_ADD_GROUP = false;
    private boolean FLAG_ADD_CHANNEL = false;
    private boolean FLAG_ADD_HASHTAG = false;
    private final BotConfig config;
    private final AppUserDAO appUserDAO;
    private final ChannelDAO channelDAO;
    private final WorkingGroupDAO workingGroupDAO;
    private final HashtagDAO hashtagDAO;
    private final HelpRequestImpl helpRequest;
    private Long workingGroupId;
    private Long channelId;
    private Long hashtagId;

    public TelegramBot(
            BotConfig config, AppUserDAO appUserDAO,
            ChannelDAO channelDAO, WorkingGroupDAO workingGroupDAO,
            HashtagDAO hashtagDAO, HelpRequestImpl helpRequest
    ) {
        this.appUserDAO = appUserDAO;
        this.channelDAO = channelDAO;
        this.workingGroupDAO = workingGroupDAO;
        this.hashtagDAO = hashtagDAO;
        this.helpRequest = helpRequest;
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        sentBotCommand();
        if (update.hasMessage() && update.getMessage().hasText()
                && update.getMessage().getForwardFromChat() == null) {
            AppUser appUser = findOrSaveAppUser(update);
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            if (START.equals(messageText) || START.name().equals(messageText)) {
                startCommandReceived(chatId);
            }
            else if (HELP.equals(messageText) || HELP.name().equals(messageText)) {
                executeMessage(helpRequest.help(chatId.toString()));
            }
            else if (MY_WORKING_GROUPS.equals(messageText)
                    || MY_WORKING_GROUPS.name().equals(messageText)) {
                executeMessage(inlineKeyboardMyGroup(chatId, appUser));
            }
            else if (FLAG_ADD_HASHTAG && messageText.matches(HASHTAG)) {
                FLAG_ADD_HASHTAG = false;
                prepareAndSendMessage(chatId, addHashtag(messageText));
            }
            else {
                FLAG_ADD_HASHTAG = false;
                prepareAndSendMessage(chatId, "Wtf dude, noname command!");
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            AppUser appUser = appUserDAO.findAppUserByTelegramUserId(update.getCallbackQuery().getFrom().getId());
            long chatCallbackId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.contains(ADD_NEW_CHANNEL.toString())) {
                FLAG_ADD_GROUP = false;
                FLAG_ADD_CHANNEL = true;
                prepareAndSendMessage(
                        chatCallbackId,
                        "1) Make the bot an administrator of the channel \n " +
                                "2) Forward the post from the channel"
                );
            }
            else if (callbackData.contains(DELETE_GROUP.toString())) {
                String groupName = removeGroup(appUser);
                prepareAndSendMessage(chatCallbackId, String.format("\"%s\" group has been deleted", groupName));
//                prepareAndSendMessage(chatCallbackId, NOT_IMPLEMENTED);
            }
            else if (ADD_WORKING_GROUP.equals(callbackData)) {
                FLAG_ADD_CHANNEL = false;
                FLAG_ADD_GROUP = true;
                prepareAndSendMessage(
                        chatCallbackId,
                        "1) Make the bot an administrator of the channel \n " +
                                "2) Forward the post from the channel"
                );
            }
            else if (REMOVE_CHANNEL.equals(callbackData)) {
                String channelName = removeChannel();
                prepareAndSendMessage(chatCallbackId, String.format("\"%s\" channel has been deleted", channelName));
            }
            else if (ADD_HASHTAG.equals(callbackData)) {
                FLAG_ADD_HASHTAG = true;
                prepareAndSendMessage(chatCallbackId, "Posts containing this hashtag will be ignored " +
                        "\nEnter hashtag");
            }
            else if (REMOVE_HASHTAG.equals(callbackData)) {
                prepareAndSendMessage(chatCallbackId, removeHashtag());
                executeMessage(inlineKeyboardChannelsList(chatCallbackId));
            }
            //TODO Хуйня яка додає канали і вілправляє список каналів

            else if (BACK_TO_CHANNELS_LIST.equals(callbackData)) {
                executeMessage(inlineKeyboardChannelsList(chatCallbackId));
            }
            else if (BACK_TO_GROUPS_LIST.equals(callbackData)) {
                executeMessage(inlineKeyboardMyGroup(chatCallbackId, appUser));
            }
            //TODO Хуйня яка відправляє кнопочки
            else if (callbackData.contains(FLAG_GROUP_LIST)) {
                workingGroupId = Long.valueOf(callbackData.replaceAll(FLAG_GROUP_LIST, ""));
                executeMessage(inlineKeyboardChannelsList(chatCallbackId));
            }
            else if (callbackData.contains(FLAG_CHANNEL_LIST)) {
                channelId = Long.valueOf(callbackData.replaceAll(FLAG_CHANNEL_LIST, ""));
                executeMessage(inlineKeyboardChannel(chatCallbackId));
            }
            else if (callbackData.contains(FLAG_HASHTAG)) {
                hashtagId = Long.valueOf(callbackData.replaceAll(FLAG_HASHTAG, ""));
                executeMessage(inlineKeyboardHashtag(chatCallbackId));
            }
        } //TODO Хуйня яка репостить пости з таргет груп
        else if (update.hasChannelPost()) {
            Channel channel = channelDAO.findChannelByChannelId(update.getChannelPost().getChatId());
            if (channel == null) {
                return;
            }
            if (update.getChannelPost().getMediaGroupId() != null) {
//                Timer sendTimer = new Timer(1000);
                SendMediaGroup mediaGroup = new SendMediaGroup();
                List<InputMedia> inputMedias = new ArrayList<>();
                inputMedias.add(InputMediaPhoto.builder().caption(update.getChannelPost()
                        .getCaption()).media(update.getChannelPost().getPhoto()
                        .get(update.getChannelPost().getPhoto().size() - 1).getFileId()).build());

                inputMedias.add(InputMediaPhoto.builder().media(update.getChannelPost().getPhoto()
                        .get(update.getChannelPost().getPhoto().size() - 1).getFileId()).build());
               mediaGroup.setChatId(update.getChannelPost().getChatId());
               mediaGroup.setMedias(inputMedias);
                try {
                    execute(mediaGroup);
                } catch (TelegramApiException e) {
                    log.error(ERROR_TEXT + e.getMessage());
                }
            } else {
                List<WorkingGroup> listGroup = channel.getWorkingGroups();
                List<Hashtag> hashtagList = channel.getHashtags();
                for (WorkingGroup group : listGroup) {
                    if (captionValidator(update, group, hashtagList)) {
                        if(update.getChannelPost().getForwardFromChat() != null){
                            ForwardMessage forwardMessage = new ForwardMessage();
                            forwardMessage.setChatId(group.getChannelId());
                            forwardMessage.setFromChatId(update.getChannelPost().getChatId());
                            forwardMessage.setMessageId(update.getChannelPost().getMessageId());
                            executeMessage(forwardMessage);
                        } else {
                            CopyMessage copyMessage = new CopyMessage();
                            copyMessage.setChatId(group.getChannelId());
                            copyMessage.setFromChatId(update.getChannelPost().getChatId());
                            copyMessage.setMessageId(update.getChannelPost().getMessageId());
                            try {
                                execute(copyMessage);
                            } catch (TelegramApiException e) {
                                log.error(ERROR_TEXT + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        //TODO Хуйня яка створює групи
        else if (update.hasMessage() && update.getMessage().getForwardFromChat() != null && FLAG_ADD_GROUP) {
            String channelTitle = update.getMessage().getForwardFromChat().getTitle();
            Long channelId = update.getMessage().getForwardFromChat().getId();
            AppUser appUser = findOrSaveAppUser(update);
            List<WorkingGroup> listGroup = appUser.getWorkingGroups();

            for (WorkingGroup group : listGroup) {
                if (group.getChannelId().equals(channelId)) {
                    executeMessage(new SendMessage(update.getMessage().getChatId().toString()
                            , "This channel has already been added"));
                    return;
                }
            }
            prepareAndSendMessage(
                    update.getMessage().getChatId(),
                    "Group added title: " + channelTitle + " "
                            + "Channel id: " + channelId
            );
            addWorkingGroup(channelTitle, channelId, appUser, listGroup);
            FLAG_ADD_GROUP = false;
        }
        else if (update.hasMessage() && update.getMessage().getForwardFromChat() != null && FLAG_ADD_CHANNEL) {
            String channelTitle = update.getMessage().getForwardFromChat().getTitle();
            Long channelId = update.getMessage().getForwardFromChat().getId();
            WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
            List<Channel> listChannel = workingGroup.getChannels();
            for (Channel group : listChannel) {
                if (group.getChannelId().equals(channelId)) {
                    executeMessage(new SendMessage(update.getMessage().getChatId().toString()
                            , "This channel has already been added"));
                    return;
                }
            }
            prepareAndSendMessage(
                    update.getMessage().getChatId(),
                    "Channel added title: " + channelTitle + " "
                            + "Channel id: " + channelId
            );
            addChannel(channelTitle, channelId, workingGroup, listChannel);
            FLAG_ADD_CHANNEL = false;
        }
    }

    private boolean hashtagIsPresent(String hashtag, WorkingGroup workingGroup) {
        List<String> listHashtag =
                hashtagDAO.getHashtagsByWorkingGroup(workingGroup)
                        .stream()
                        .map(Hashtag::getHashtagName)
                        .toList();
        return listHashtag.contains(hashtag);
    }

    public String removeGroup(AppUser appUser) {
        WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
        appUser.getWorkingGroups().remove(workingGroup);
        appUserDAO.save(appUser);
        workingGroupDAO.delete(workingGroup);
        return workingGroup.getNameChannel();
    }

    public String removeChannel() {
        Channel channel = channelDAO.findChannelById(channelId);
        WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
        WorkingGroup oldWorkingGroup = channel.getWorkingGroups().get(0);
        workingGroup.getChannels().remove(channel);
        workingGroupDAO.save(workingGroup);
        channel.getWorkingGroups().remove(oldWorkingGroup);
        channelDAO.save(channel);
        return channel.getChannelName();
    }
    private String removeHashtag() {
        Hashtag hashtag = hashtagDAO.getHashtagsById(hashtagId);
        Channel channel = channelDAO.findChannelById(channelId);
        channel.getHashtags().remove(hashtag);
        channelDAO.save(channel);
        hashtagDAO.delete(hashtag);
        return String.format("Hashtag %s has ben deleted",hashtag.getHashtagName());
    }

    private void addWorkingGroup(String title, Long channelId, AppUser appUser, List<WorkingGroup> listGroup) {
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

    private void addChannel(String title, Long channelId, WorkingGroup workingGroup, List<Channel> listChanel) {
        Channel channel = channelDAO.findChannelByChannelId(channelId);
        if (channel != null) {
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

    private String addHashtag(String messageText) {
        if (hashtagIsPresent(messageText, workingGroupDAO.findWorkingGroupById(workingGroupId))) {
            return "This hashtag already exists";
        }
        Hashtag newHashtag = Hashtag.builder()
                .hashtagName(messageText)
                .workingGroup(workingGroupDAO.findWorkingGroupById(workingGroupId))
                .build();
        hashtagDAO.save(newHashtag);
        Channel channel = channelDAO.findChannelById(channelId);
        channel.getHashtags().add(newHashtag);
        channelDAO.save(channel);

        return String.format("Hashtag %s added ", messageText);
    }

    private void sentBotCommand() {
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand(START.toString(), "get a welcome message"));
        listOfCommands.add(new BotCommand(HELP.toString(), "info how to use this bot"));
        listOfCommands.add(new BotCommand(MY_WORKING_GROUPS.toString(), "list working groups"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private ReplyKeyboardMarkup setBotKeyboardRow() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rowList = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(START.name());
        row.add(HELP.name());
        row.add(MY_WORKING_GROUPS.name());
        rowList.add(row);
        markup.setKeyboard(rowList);
        return markup;
    }

    //TODO дуже важна штука кст
    private void deleteMessage(Integer chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e);
        }
    }

    private AppUser findOrSaveAppUser(Update update) {
        Long userId = getUserId(update);
        User telegramUser = update.getMessage().getFrom();
        AppUser persistentAppUser = appUserDAO.findAppUserByTelegramUserId(userId);
        if (persistentAppUser == null) {
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

    private Long getUserId(Update update) {
        return update.getMessage().getFrom().getId();
    }

    private <T extends BotApiMethodMessage> void executeMessage(T message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void startCommandReceived(long chatId) {
        SendMessage message = new SendMessage(String.valueOf(chatId), "Hi loh!");
        message.setReplyMarkup(setBotKeyboardRow());
        message.enableMarkdown(true);
        executeMessage(message);
    }

    private boolean captionValidator(Update update, WorkingGroup workingGroup, List<Hashtag> hashtagList) {
        List<String> listHashtags =
                hashtagDAO.getHashtagsByWorkingGroup(workingGroup).stream()
                        .filter(hashtagList::contains)
                        .map(Hashtag::getHashtagName)
                        .toList();

        return (!update.getChannelPost().hasEntities()
                || update.getChannelPost().getEntities().stream()
                .noneMatch(e -> listHashtags.contains(e.getText())))
                && (update.getChannelPost().getCaptionEntities() == null
                || update.getChannelPost().getCaptionEntities().stream()
                .noneMatch(e -> listHashtags.contains(e.getText())));
    }

    private SendMessage inlineKeyboardChannelsList(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(workingGroupDAO.findWorkingGroupById(workingGroupId).getNameChannel()
                + " group");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<Channel> listGroup = workingGroupDAO.findWorkingGroupById(workingGroupId).getChannels();
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
        rowsInLine.add(inlineButtonBuilder("❌ Delete group", DELETE_GROUP.toString()));
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

    private SendMessage inlineKeyboardChannel(Long chatId) {
        SendMessage sendMessage = new SendMessage();
         Channel channel = channelDAO.findChannelById(channelId);
        sendMessage.setText( channel.getChannelName()
                + " group");
        sendMessage.setChatId(chatId);
        List<Hashtag> hashtagList =  channel.getHashtags();
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

    private SendMessage inlineKeyboardMyGroup(Long chatId, AppUser appUser) {
        SendMessage sendMessage = new SendMessage();
        List<WorkingGroup> listGroup = appUser.getWorkingGroups();
        sendMessage.setChatId(chatId);
        sendMessage.setText("My list working group");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        for (int i = 0; i < listGroup.size(); ) {
            StringBuilder strBuilder = new StringBuilder();
            WorkingGroup workingGroup = listGroup.get(i);
            strBuilder.append(++i)
                    .append(") Title (")
                    .append(workingGroup.getNameChannel())
                    .append(") id(")
                    .append(workingGroup.getChannelId())
                    .append(")");
            rowsInLine.add(inlineButtonBuilder(
                    strBuilder.toString(),
                    FLAG_GROUP_LIST + workingGroup.getId().toString()
            ));
        }
        rowsInLine.add(inlineButtonBuilder("Add group", ADD_WORKING_GROUP.toString()));
        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);
        return sendMessage;
    }

    private SendMessage inlineKeyboardHashtag(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(hashtagDAO.getHashtagsById(hashtagId).getHashtagName()
                + " hashtag");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        rowsInLine.add(inlineButtonBuilder("↪️Back", BACK_TO_CHANNELS_LIST.toString()));
        rowsInLine.add(inlineButtonBuilder("❌ Remove target hashtag", REMOVE_HASHTAG.toString()));
        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);
        return sendMessage;
    }
}
