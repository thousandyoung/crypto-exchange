package com.howellyoung.exchange.message;

import com.howellyoung.exchange.message.base.BaseMessage;

public class NotificationMessage extends BaseMessage {

    public String type;

    public Long userId;

    public Object data;
}
