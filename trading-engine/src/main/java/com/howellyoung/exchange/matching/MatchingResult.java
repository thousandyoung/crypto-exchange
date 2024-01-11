package com.howellyoung.exchange.matching;

import com.howellyoung.exchange.order.entity.OrderEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/*
    MatchingResult is the matching result of a takerOrder, matching with makerOrderBook
 */
public class MatchingResult {
    public final OrderEntity takerOrder;
    public final List<MatchingDetailRecord> matchingDetails = new ArrayList<>();

    public MatchingResult(OrderEntity takerOrder) {
        this.takerOrder = takerOrder;
    }

    public void add(BigDecimal price, BigDecimal matchedQuantity, OrderEntity makerOrder) {
        matchingDetails.add(new MatchingDetailRecord(price, matchedQuantity, this.takerOrder, makerOrder));
    }

    @Override
    public String toString() {
        if (matchingDetails.isEmpty()) {
            return "no matched.";
        }
        return matchingDetails.size() + " matched: "
                + String.join(", ", this.matchingDetails.stream().map(MatchingDetailRecord::toString).toArray(String[]::new));
    }
}
