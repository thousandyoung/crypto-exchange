package com.howellyoung.exchange.matching;

import com.howellyoung.exchange.order.entity.OrderEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;



public class MatchingResult {

    public final OrderEntity takerOrder;
    public final List<MatchingDetailRecord> matchDetails = new ArrayList<>();

    public MatchingResult(OrderEntity takerOrder) {
        this.takerOrder = takerOrder;
    }

    public void add(BigDecimal price, BigDecimal matchedQuantity, OrderEntity makerOrder) {
        matchDetails.add(new MatchingDetailRecord(price, matchedQuantity, this.takerOrder, makerOrder));
    }

    @Override
    public String toString() {
        if (matchDetails.isEmpty()) {
            return "no matched.";
        }
        return matchDetails.size() + " matched: "
                + String.join(", ", this.matchDetails.stream().map(MatchingDetailRecord::toString).toArray(String[]::new));
    }
}
