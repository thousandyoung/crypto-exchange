package com.howellyoung.exchange.message;

import java.util.List;

import com.howellyoung.exchange.entity.quotation.TickEntity;
import com.howellyoung.exchange.message.base.BaseMessage;


public class TickMessage extends BaseMessage {

    public long sequenceId;

    public List<TickEntity> ticks;

}
