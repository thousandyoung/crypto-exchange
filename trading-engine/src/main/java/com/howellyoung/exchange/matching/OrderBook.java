package com.howellyoung.exchange.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import com.howellyoung.exchange.bean.OrderBookItemBean;
import com.howellyoung.exchange.enums.OrderDirectionEnum;
import com.howellyoung.exchange.entity.trade.OrderEntity;


import java.util.TreeMap;

public class OrderBook {

    public final OrderDirectionEnum direction;
    public final TreeMap<OrderKey, OrderEntity> orderMap;

    public OrderBook(OrderDirectionEnum direction) {
        this.direction = direction;
        this.orderMap = new TreeMap<>(direction == OrderDirectionEnum.BID ? SORT_BID : SORT_ASK);
    }

    public OrderEntity getFirst() {
        return this.orderMap.isEmpty() ? null : this.orderMap.firstEntry().getValue();
    }

    public boolean remove(OrderEntity order) {
        return this.orderMap.remove(new OrderKey(order.sequenceId, order.price)) != null;
    }

    public boolean add(OrderEntity order) {
        return this.orderMap.put(new OrderKey(order.sequenceId, order.price), order) == null;
    }

    public boolean exist(OrderEntity order) {
        return this.orderMap.containsKey(new OrderKey(order.sequenceId, order.price));
    }

    public int size() {
        return this.orderMap.size();
    }

    /*
        Create OrderBook by using ordered OrderEntity and generate OrderBookItemBean.
     */
    public List<OrderBookItemBean> getOrderBook(int maxDepth) {
        List<OrderBookItemBean> items = new ArrayList<>(maxDepth);
        OrderBookItemBean prevItem = null;
        for (OrderKey key : this.orderMap.keySet()) {
            OrderEntity order = this.orderMap.get(key);
            if (prevItem == null) {
                prevItem = new OrderBookItemBean(order.price, order.unfilledQuantity);
                items.add(prevItem);
            } else {
                if (order.price.compareTo(prevItem.price) == 0) {
                    prevItem.addQuantityToVolume(order.unfilledQuantity);
                } else {
                    if (items.size() >= maxDepth) {
                        break;
                    }
                    prevItem = new OrderBookItemBean(order.price, order.unfilledQuantity);
                    items.add(prevItem);
                }
            }
        }
        return items;
    }

    @Override
    public String toString() {
        if (this.orderMap.isEmpty()) {
            return "(empty)";
        }
        List<String> orders = new ArrayList<>(10);
        for (Entry<OrderKey, OrderEntity> entry : this.orderMap.entrySet()) {
            OrderEntity order = entry.getValue();
            orders.add("  " + order.price + " " + order.unfilledQuantity + " " + order.toString());
        }
        if (direction == OrderDirectionEnum.ASK) {
            Collections.reverse(orders);
        }
        return String.join("\n", orders);
    }

    private static final Comparator<OrderKey> SORT_ASK = new Comparator<>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格低优先:
            int cmp = o1.price().compareTo(o2.price());
            // 时间早优先：
            return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
        }
    };

    private static final Comparator<OrderKey> SORT_BID = new Comparator<>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格高优先:
            int cmp = o2.price().compareTo(o1.price());
            // 时间早优先:
            return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
        }
    };
}
