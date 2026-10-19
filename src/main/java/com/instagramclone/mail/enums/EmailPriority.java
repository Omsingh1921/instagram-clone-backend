package com.instagramclone.mail.enums;

public enum EmailPriority {
    CRITICAL(1),
    HIGH(2),
    MEDIUM(3),
    LOW(4);

    private final int level;

    EmailPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
