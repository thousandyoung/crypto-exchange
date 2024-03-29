package com.howellyoung.exchange.match;

import com.howellyoung.exchange.enums.OrderDirectionEnum;
import com.howellyoung.exchange.enums.OrderStatusEnum;
import com.howellyoung.exchange.matching.MatchingDetailRecord;
import com.howellyoung.exchange.matching.MatchingResult;
import com.howellyoung.exchange.matching.MatchingService;
import com.howellyoung.exchange.entity.trade.OrderEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchingServiceTest {

    static Long USER_A = 12345L;
    long sequenceId = 0;
    MatchingService matchingService;

    @BeforeEach
    void init() {
        this.matchingService = new MatchingService();
    }

    @Test
    void processOrders() {
        List<OrderEntity> orders = List.of(
                createOrder(OrderDirectionEnum.BID, "12300.21", "1.02"), // 0
                createOrder(OrderDirectionEnum.BID, "12305.39", "0.33"), // 1
                createOrder(OrderDirectionEnum.ASK, "12305.39", "0.11"), // 2
                createOrder(OrderDirectionEnum.ASK, "12300.01", "0.33"), // 3
                createOrder(OrderDirectionEnum.ASK, "12400.00", "0.10"), // 4
                createOrder(OrderDirectionEnum.ASK, "12400.00", "0.20"), // 5
                createOrder(OrderDirectionEnum.ASK, "12390.00", "0.15"), // 6
                createOrder(OrderDirectionEnum.BID, "12400.01", "0.55"), // 7
                createOrder(OrderDirectionEnum.BID, "12300.00", "0.77")); // 8
        List<MatchingDetailRecord> matches = new ArrayList<>();
        for (OrderEntity order : orders) {
            MatchingResult mr = this.matchingService.processOrder(order);
            matches.addAll(mr.matchingDetails);
        }
        assertArrayEquals(new MatchingDetailRecord[] { //
                new MatchingDetailRecord(bd("12305.39"), bd("0.11"), orders.get(2), orders.get(1)), //
                new MatchingDetailRecord(bd("12305.39"), bd("0.22"), orders.get(3), orders.get(1)), //
                new MatchingDetailRecord(bd("12300.21"), bd("0.11"), orders.get(3), orders.get(0)), //
                new MatchingDetailRecord(bd("12390.00"), bd("0.15"), orders.get(7), orders.get(6)), //
                new MatchingDetailRecord(bd("12400.00"), bd("0.10"), orders.get(7), orders.get(4)), //
                new MatchingDetailRecord(bd("12400.00"), bd("0.20"), orders.get(7), orders.get(5)), //
        }, matches.toArray(MatchingDetailRecord[]::new));
        assertTrue(bd("12400.00").compareTo(matchingService.marketPrice) == 0);
    }

    OrderEntity createOrder(OrderDirectionEnum direction, String price, String quantity) {
        this.sequenceId++;
        var order = new OrderEntity();
        order.id = this.sequenceId << 4;
        order.sequenceId = this.sequenceId;
        order.direction = direction;
        order.price = bd(price);
        order.quantity = order.unfilledQuantity = bd(quantity);
        order.status = OrderStatusEnum.PENDING;
        order.userId = USER_A;
        order.createdAt = order.updatedAt = 1234567890000L + this.sequenceId;
        return order;
    }

    BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

}
