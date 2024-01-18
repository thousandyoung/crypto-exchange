package com.howellyoung.exchange.messaging;

import java.util.List;

import com.howellyoung.exchange.message.base.BaseMessage;

@FunctionalInterface
public interface MessageProducer<T extends BaseMessage> {

    void sendMessage(T message);

    default void sendMessages(List<T> messages) {
        for (T message : messages) {
            sendMessage(message);
        }
    }
}
