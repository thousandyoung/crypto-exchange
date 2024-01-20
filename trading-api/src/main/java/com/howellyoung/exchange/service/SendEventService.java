package com.howellyoung.exchange.service;

import com.howellyoung.exchange.message.event.BaseEvent;
import com.howellyoung.exchange.messaging.MessageProducer;
import com.howellyoung.exchange.messaging.Messaging;
import com.howellyoung.exchange.messaging.MessagingFactory;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class SendEventService {

    @Autowired
    private MessagingFactory messagingFactory;

    private MessageProducer<BaseEvent> messageProducer;

    @PostConstruct
    public void init() {
        this.messageProducer = messagingFactory.createMessageProducer(Messaging.Topic.SEQUENCE, BaseEvent.class);
    }

    public void sendMessage(BaseEvent message) {
        this.messageProducer.sendMessage(message);
    }
}
