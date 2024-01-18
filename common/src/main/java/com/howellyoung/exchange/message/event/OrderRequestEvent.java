package com.howellyoung.exchange.message.event;

import com.howellyoung.exchange.enums.OrderDirectionEnum;

import java.math.BigDecimal;

public class OrderRequestEvent extends BaseEvent {

    public Long userId;

    public OrderDirectionEnum direction;

    public BigDecimal price;

    public BigDecimal quantity;

    @Override
    public String toString() {
        return "OrderRequestEvent [sequenceId=" + sequenceId + ", previousId=" + previousId + ", uniqueId=" + uniqueId
                + ", refId=" + refId + ", createdAt=" + createdAt + ", userId=" + userId + ", direction=" + direction
                + ", price=" + price + ", quantity=" + quantity + "]";
    }
}
