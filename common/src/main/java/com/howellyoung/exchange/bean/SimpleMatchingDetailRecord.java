package com.howellyoung.exchange.bean;

import com.howellyoung.exchange.enums.MatchTypeEnum;

import java.math.BigDecimal;

public record SimpleMatchingDetailRecord(BigDecimal price, BigDecimal quantity, MatchTypeEnum type) {
}
