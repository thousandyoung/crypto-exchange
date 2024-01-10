package com.howellyoung.exchange.bean;

import java.math.BigDecimal;
/*
   OrderBookItem is a item in OrderBook, each represents a price and volume,
   volume is the sum of quantity of all orders with the same price.
   In other words, each OrderBookItem represents a price level, which is the orders with the same price.
 */
public class OrderBookItemBean {

    public BigDecimal price;
    public BigDecimal volume;

    public OrderBookItemBean(BigDecimal price, BigDecimal volume) {
        this.price = price;
        this.volume = volume;
    }

    public void addQuantityToVolume(BigDecimal quantity) {
        this.volume = this.volume.add(quantity);
    }
}
