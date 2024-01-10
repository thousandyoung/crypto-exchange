package com.howellyoung.exchange.matching;

import java.math.BigDecimal;

public record OrderKey(long sequenceId, BigDecimal price) {
}
