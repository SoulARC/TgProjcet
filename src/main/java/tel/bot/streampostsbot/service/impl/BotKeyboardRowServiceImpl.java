package tel.bot.streampostsbot.service.impl;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import tel.bot.streampostsbot.service.BotKeyboardRowService;

import java.util.ArrayList;
import java.util.List;

import static tel.bot.streampostsbot.service.enums.ServiceCommands.HELP;
import static tel.bot.streampostsbot.service.enums.ServiceCommands.MY_WORKING_GROUPS;
import static tel.bot.streampostsbot.service.enums.ServiceCommands.START;

@Service
public class BotKeyboardRowServiceImpl implements BotKeyboardRowService {

    @Override
    public ReplyKeyboardMarkup setBotKeyboardRow() {
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
}
