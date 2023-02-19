package tel.bot.streampostsbot.service.enums;

public enum ChannelCommands {
    REMOVE_CHANNEL("/remove_channel"),
    ADD_HASHTAG("/add_hashtag"),
    REMOVE_HASHTAG("/remove_hashtag"),
    BACK_TO_CHANNELS_LIST("/back_to_channels_list");

    private final String str;

    ChannelCommands(String str) {
        this.str = str;
    }

    public boolean equals(String str) {
        return this.str.equals(str);
    }

    @Override
    public String toString() {
        return str;
    }

}
