package tel.bot.streampostsbot.service.enums;

public enum ServiceCommands {
    START("/start"),
    HELP("/help"),
    MY_WORKING_GROUPS("/my_working_groups");

    private final String str;

    ServiceCommands(String str) {
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
