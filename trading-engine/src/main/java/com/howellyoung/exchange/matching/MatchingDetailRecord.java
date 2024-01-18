package com.howellyoung.exchange.matching;

import java.math.BigDecimal;

import com.howellyoung.exchange.entity.trade.OrderEntity;
public record MatchingDetailRecord(BigDecimal price, BigDecimal quantity, OrderEntity takerOrder, OrderEntity makerOrder) {
}
