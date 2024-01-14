package com.howellyoung.exchange.messaging;

@FunctionalInterface
public interface MessageConsumer {

    void stop(); // stop and release resources

}
