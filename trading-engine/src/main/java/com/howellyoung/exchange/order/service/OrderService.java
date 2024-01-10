package com.howellyoung.exchange.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import com.howellyoung.exchange.enums.AssetEnum;
import com.howellyoung.exchange.order.entity.OrderEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.howellyoung.exchange.assets.AssetService;
import com.howellyoung.exchange.enums.DirectionEnum;

/**
 * 订单服务, active order means order that is not fully filled,
 * once it is fully filled, it will be removed from our memory based map.
 */
@Component
public class OrderService {

    final AssetService assetService;

    public OrderService(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }
    // 跟踪所有活动订单:
    final ConcurrentMap<Long, OrderEntity> allActiveOrdersMap = new ConcurrentHashMap<>();

    // 跟踪用户活动订单:
    final ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> allUserOrdersMap = new ConcurrentHashMap<>();

    /**
     * 创建订单，失败返回null:
     */
    public OrderEntity createOrder(long sequenceId, long timeStamp, Long orderId, Long userId, DirectionEnum direction,
            BigDecimal price, BigDecimal quantity) {
        switch (direction) {
        case BID -> {
            // 买入，需冻结USD：
            if (!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                return null;
            }
        }
        case ASK -> {
            // 卖出，需冻结BTC：
            if (!assetService.tryFreeze(userId, AssetEnum.BTC, quantity)) {
                return null;
            }
        }
        default -> throw new IllegalArgumentException("Invalid direction.");
        }
        OrderEntity order = new OrderEntity();
        order.id = orderId;
        order.sequenceId = sequenceId;
        order.userId = userId;
        order.direction = direction;
        order.price = price;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createdAt = order.updatedAt = timeStamp;
        // 添加到ActiveOrdersMap:
        this.allActiveOrdersMap.put(order.id, order);
        // 给当前用户添加该订单:
        ConcurrentMap<Long, OrderEntity> userOrdersMap = this.allUserOrdersMap.get(userId);
        if (userOrdersMap == null) {
            userOrdersMap = new ConcurrentHashMap<>();
            this.allUserOrdersMap.put(userId, userOrdersMap);
        }
        userOrdersMap.put(order.id, order);
        return order;
    }

    public ConcurrentMap<Long, OrderEntity> getAllActiveOrdersMap() {
        return this.allActiveOrdersMap;
    }

    public OrderEntity getOrder(Long orderId) {
        return this.allActiveOrdersMap.get(orderId);
    }

    public ConcurrentMap<Long, OrderEntity> getOrdersForUser(Long userId) {
        return this.allUserOrdersMap.get(userId);
    }

    // 删除活动订单:
    public void removeOrder(Long orderId) {
        // 从ActiveOrders中删除:
        OrderEntity removed = this.allActiveOrdersMap.remove(orderId);
        if (removed == null) {
            throw new IllegalArgumentException("Order not found by orderId in active orders: " + orderId);
        }
        // 从UserOrders中删除:
        ConcurrentMap<Long, OrderEntity> userOrders = allUserOrdersMap.get(removed.userId);
        if (userOrders == null) {
            throw new IllegalArgumentException("User orders not found by userId: " + removed.userId);
        }
        if (userOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders: " + orderId);
        }
    }

    public void debug() {
        System.out.println("---------- orders ----------");
        List<OrderEntity> orders = new ArrayList<>(this.allActiveOrdersMap.values());
        Collections.sort(orders);
        for (OrderEntity order : orders) {
            System.out.println("  " + order.id + " " + order.direction + " price: " + order.price + " unfilled: "
                    + order.unfilledQuantity + " quantity: " + order.quantity + " sequenceId: " + order.sequenceId
                    + " userId: " + order.userId);
        }
        System.out.println("---------- // orders ----------");
    }
}
