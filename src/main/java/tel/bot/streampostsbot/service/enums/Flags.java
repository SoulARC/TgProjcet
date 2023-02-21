package tel.bot.streampostsbot.service.enums;

public enum Flags {
    FLAG_CHANNEL_LIST("channelList "),
    FLAG_GROUP_LIST("groupList "),
    FLAG_HASHTAG("hashtag ");

    private final String str;

    Flags(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
