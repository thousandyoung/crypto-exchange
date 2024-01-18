package com.howellyoung.exchange.message.event;

import org.springframework.lang.Nullable;
import com.howellyoung.exchange.message.base.BaseMessage;

public class BaseEvent extends BaseMessage {
    /**
     * Message id, set after sequenced.
     */
    public long sequenceId;

    /**
     * Previous message sequence id.
     */
    public long previousId;

    /**
     * Unique ID or null if not set.
     */
    @Nullable
    public String uniqueId;
}
