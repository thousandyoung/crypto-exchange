package com.howellyoung.exchange.bean;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.howellyoung.exchange.util.JsonUtil;

public class OrderBookBean {

    public static final String EMPTY = JsonUtil.convertObjectToJsonString(new OrderBookBean(0, BigDecimal.ZERO, List.of(), List.of()));

    @JsonIgnore
    public long sequenceId;

    public BigDecimal price;

    public List<OrderBookItemBean> bidOrderBook;

    public List<OrderBookItemBean> askOrderBook;

    public OrderBookBean(long sequenceId, BigDecimal price, List<OrderBookItemBean> buy, List<OrderBookItemBean> sell) {
        this.sequenceId = sequenceId;
        this.price = price;
        this.bidOrderBook = buy;
        this.askOrderBook = sell;
    }
}
