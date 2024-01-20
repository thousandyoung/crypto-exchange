package com.howellyoung.exchange.message.base;

public class BaseMessage {
    /**
     * Reference id, or null if not set. track message to find DeferredResult for response.
     */
    public String refId = null;

    /**
     * Message created at.
     */
    public long createdAt;
}
