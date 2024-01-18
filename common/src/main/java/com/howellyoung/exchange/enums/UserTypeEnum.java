package com.howellyoung.exchange.enums;

/**
 * User type enumeration.
 */
public enum UserTypeEnum {

    SYSTEM(1),

    TRADER(0);

    /**
     * User id
     */
    private final long userId;

    public long getInternalUserId() {
        return this.userId;
    }

    UserTypeEnum(long userId) {
        this.userId = userId;
    }
}
