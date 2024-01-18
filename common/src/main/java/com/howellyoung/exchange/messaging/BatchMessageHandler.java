package com.howellyoung.exchange.messaging;

import com.howellyoung.exchange.message.base.BaseMessage;

import java.util.List;


@FunctionalInterface
public interface BatchMessageHandler<T extends BaseMessage> {

    void processMessages(List<T> messages);

}
