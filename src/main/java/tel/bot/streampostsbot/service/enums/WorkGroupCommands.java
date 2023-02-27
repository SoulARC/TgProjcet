package tel.bot.streampostsbot.service.enums;

public enum WorkGroupCommands {
    ADD_WORKING_GROUP("/add_working_group"),
    ADD_NEW_CHANNEL("/add_new_channel"),
    DELETE_MAIN_CHANNEL("/delete_group"),
    BACK_TO_GROUPS_LIST("/back_to_groups_list");

    private final String str;

    WorkGroupCommands(String str) {
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
