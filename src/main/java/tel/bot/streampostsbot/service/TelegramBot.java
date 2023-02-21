package tel.bot.streampostsbot.service;

import static tel.bot.streampostsbot.service.enums.ChannelCommands.ADD_HASHTAG;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.BACK_TO_CHANNELS_LIST;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.REMOVE_CHANNEL;
import static tel.bot.streampostsbot.service.enums.ChannelCommands.REMOVE_HASHTAG;
import static tel.bot.streampostsbot.service.enums.Flags.FLAG_CHANNEL_LIST;
import static tel.bot.streampostsbot.service.enums.Flags.FLAG_GROUP_LIST;
import static tel.bot.streampostsbot.service.enums.Flags.FLAG_HASHTAG;
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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final String ERROR_TEXT = "Error occurred: ";
    private static final String HASHTAG = "^#(?=.*[^0-9])[a-zа-яёіїґ0-9]{1,29}$";
    private static final String NOT_IMPLEMENTED = "Sorry, this feature is not yet implemented :(";
    private static final int INITIAL_LIMIT = 1; //TODO якщо комусь знадобиться за раз відправти 20+ фото, бажано підвищити ліміт
    private boolean FLAG_ADD_GROUP = false;
    private boolean FLAG_ADD_CHANNEL = false;
    private boolean FLAG_ADD_HASHTAG = false;
    private final BotConfig config;
    private final AppUserDAO appUserDAO;
    private final ChannelDAO channelDAO;
    private final WorkingGroupDAO workingGroupDAO;
    private final HashtagDAO hashtagDAO;
    private final HelpRequestImpl helpRequest;
    private final InlineKeyboardService keyboardService;
    private final BotManagerService botManagerService;
    private final Map<String, Map<Channel, List<Message>>> mediaGroupMap = new HashMap<>();
    private Long workingGroupId;
    private Long channelId;
    private Long hashtagId;

    public TelegramBot(
            BotConfig config, AppUserDAO appUserDAO,
            ChannelDAO channelDAO, WorkingGroupDAO workingGroupDAO,
            HashtagDAO hashtagDAO, HelpRequestImpl helpRequest,
            InlineKeyboardService inlineKeyboardService,
            BotManagerService botManagerService
    ) {
        this.appUserDAO = appUserDAO;
        this.channelDAO = channelDAO;
        this.workingGroupDAO = workingGroupDAO;
        this.hashtagDAO = hashtagDAO;
        this.helpRequest = helpRequest;
        this.config = config;
        this.keyboardService = inlineKeyboardService;
        this.botManagerService = botManagerService;
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
            AppUser appUser = botManagerService.findOrSaveAppUser(update);
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
                executeMessage(keyboardService.inlineKeyboardMyGroup(chatId, appUser));
            }
            else if (FLAG_ADD_HASHTAG && messageText.matches(HASHTAG)) {
                FLAG_ADD_HASHTAG = false;
                Channel channel = channelDAO.findChannelById(channelId);
                WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
                List<Hashtag> hashtagList = hashtagDAO.getHashtagsByWorkingGroup(workingGroup);
                if (hashtagIsPresent(messageText, hashtagList)) {
                    prepareAndSendMessage(chatId, "This hashtag already exists");
                }
                else {
                    prepareAndSendMessage(chatId, botManagerService.addHashtag(messageText, workingGroup, channel));
                }
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
//                WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
//                prepareAndSendMessage(chatCallbackId, botManagerService.removeGroup(appUser, workingGroup));
                prepareAndSendMessage(chatCallbackId, NOT_IMPLEMENTED);
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
                Channel channel = channelDAO.findChannelById(channelId);
                WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
                prepareAndSendMessage(chatCallbackId, botManagerService.removeChannel(channel, workingGroup));
            }
            else if (ADD_HASHTAG.equals(callbackData)) {
                FLAG_ADD_HASHTAG = true;
                prepareAndSendMessage(chatCallbackId, "Posts containing this hashtag will be ignored " +
                        "\nEnter hashtag");
            }
            else if (REMOVE_HASHTAG.equals(callbackData)) {
                Hashtag hashtag = hashtagDAO.getHashtagsById(hashtagId);
                Channel channel = channelDAO.findChannelById(channelId);
                prepareAndSendMessage(chatCallbackId, botManagerService.removeHashtag(hashtag, channel));
                WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
                executeMessage(keyboardService.inlineKeyboardChannelsList(chatCallbackId, workingGroup));
            }
            //TODO Хуйня яка додає канали і вілправляє список каналів

            else if (BACK_TO_CHANNELS_LIST.equals(callbackData)) {
                WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
                executeMessage(keyboardService.inlineKeyboardChannelsList(chatCallbackId, workingGroup));
            }
            else if (BACK_TO_GROUPS_LIST.equals(callbackData)) {
                executeMessage(keyboardService.inlineKeyboardMyGroup(chatCallbackId, appUser));
            }
            //TODO Хуйня яка відправляє кнопочки
            else if (callbackData.contains(FLAG_GROUP_LIST.toString())) {
                workingGroupId = Long.valueOf(callbackData.replaceAll(FLAG_GROUP_LIST.toString(), ""));
                WorkingGroup workingGroup = workingGroupDAO.findWorkingGroupById(workingGroupId);
                executeMessage(keyboardService.inlineKeyboardChannelsList(chatCallbackId, workingGroup));
            }
            else if (callbackData.contains(FLAG_CHANNEL_LIST.toString())) {
                channelId = Long.valueOf(callbackData.replaceAll(FLAG_CHANNEL_LIST.toString(), ""));
                Channel channel = channelDAO.findChannelById(channelId);
                executeMessage(keyboardService.inlineKeyboardChannel(chatCallbackId, channel));
            }
            else if (callbackData.contains(FLAG_HASHTAG.toString())) {
                hashtagId = Long.valueOf(callbackData.replaceAll(FLAG_HASHTAG.toString(), ""));
                Hashtag hashtag = hashtagDAO.getHashtagsById(hashtagId);
                executeMessage(keyboardService.inlineKeyboardHashtag(chatCallbackId, hashtag));
            }
        } //TODO Хуйня яка репостить пости з таргет груп
        else if (update.hasChannelPost()) {
            Message channelPost = update.getChannelPost();
            Channel channel = channelDAO.findChannelByChannelId(update.getChannelPost().getChatId());
            if (Optional.ofNullable(channel).isPresent()) {
                SendMediaGroup mediaGroup = null;
                List<WorkingGroup> listGroup = channel.getWorkingGroups();
                List<Hashtag> hashtagList = channel.getHashtags();
                //TODO оцю хуйню доробить, в принципі працює так як однорукий шульга
                if (channelPost.getMediaGroupId() != null) {
                    String mediaGroupId = update.getChannelPost().getMediaGroupId();
                    if (!mediaGroupMap.containsKey(mediaGroupId)) {
                        Map<Channel, List<Message>> firstMessageMap = new HashMap<>();
                        List<Message> firstMessage = new ArrayList<>();
                        firstMessage.add(update.getChannelPost());
                        firstMessageMap.put(channel, firstMessage);
                        mediaGroupMap.put(mediaGroupId, firstMessageMap);
                    }
                    else {
                        Map<Channel, List<Message>> messageMap = mediaGroupMap.get(mediaGroupId);
                        if (messageMap.containsKey(channel)) {
                            messageMap.get(channel).add(update.getChannelPost());
                        } else {
                            List<Message> messageList = new ArrayList<>();
                            messageList.add(update.getChannelPost());
                            messageMap.put(channel, messageList);
                        }
                    }
                }
                if (mediaGroupMap.size() > INITIAL_LIMIT) {
                    Channel targetChanel = mediaGroupMap.values().iterator().next().keySet().iterator().next();
                    listGroup = targetChanel.getWorkingGroups();
                    hashtagList = targetChanel.getHashtags();
                    String targetKey = Collections.max(mediaGroupMap.entrySet(), Map.Entry.comparingByValue(
                            Comparator.comparingInt(m -> m.values().stream().mapToInt(List::size).sum())
                    )).getKey();
                    mediaGroup = sendMediaGroup(mediaGroupMap.get(targetKey).values().stream().flatMap(
                            List::stream).collect(Collectors.toList()));
                    mediaGroupMap.remove(targetKey);
                }
                for (WorkingGroup group : listGroup) {
                    Long groupId = group.getChannelId();
                    if (mediaGroup != null && captionMediaGroupValidator(mediaGroup.getMedias().get(0).getCaption(),group,
                            hashtagList)) {
                        try {
                            mediaGroup.setChatId(groupId);
                            execute(mediaGroup);
                        } catch (TelegramApiException e) {
                            log.error(ERROR_TEXT + e.getMessage());
                        }
                    }
                    if (captionValidator(channelPost, group, hashtagList) &&
                            update.getChannelPost().getMediaGroupId() == null) {
                        //TODO Якщо ми пересилаєм пост з іншого каналу
                        if (update.getChannelPost().getForwardFromChat() != null) {
                            ForwardMessage forwardMessage = new ForwardMessage();
                            forwardMessage.setChatId(groupId);
                            forwardMessage.setFromChatId(update.getChannelPost().getChatId());
                            forwardMessage.setMessageId(update.getChannelPost().getMessageId());
                            executeMessage(forwardMessage);
                        }
                        else {
                            CopyMessage copyMessage = new CopyMessage();
                            copyMessage.setChatId(groupId);
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
            AppUser appUser = botManagerService.findOrSaveAppUser(update);
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
            botManagerService.addWorkingGroup(channelTitle, channelId, appUser, listGroup);
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
            botManagerService.addChannel(channelTitle, channelId, workingGroup, listChannel);
            FLAG_ADD_CHANNEL = false;
        }
    }

    private boolean captionMediaGroupValidator(
            String caption, WorkingGroup group, List<Hashtag> hashtagList
    ) {
        List<String> listHashtags =
                hashtagDAO.getHashtagsByWorkingGroup(group).stream()
                        .filter(hashtagList::contains)
                        .map(Hashtag::getHashtagName)
                        .toList();

        return (caption == null
                || listHashtags.stream()
                .noneMatch(caption::contains));
    }

    private SendMediaGroup sendMediaGroup(List<Message> mediaGroupList) {
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

    private boolean hashtagIsPresent(String hashtag, List<Hashtag> hashtagList) {
        List<String> listHashtag = hashtagList.stream()
                .map(Hashtag::getHashtagName)
                .toList();
        return listHashtag.contains(hashtag);
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

    private boolean captionValidator(Message channelPost, WorkingGroup workingGroup, List<Hashtag> hashtagList) {
        List<String> listHashtags =
                hashtagDAO.getHashtagsByWorkingGroup(workingGroup).stream()
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

    //TODO дуже важна штука кст
    private void deleteMessage(Integer chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e);
        }
    }
}
