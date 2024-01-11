package com.howellyoung.exchange.matching;

import java.math.BigDecimal;

import com.howellyoung.exchange.bean.OrderBookBean;
import com.howellyoung.exchange.enums.OrderDirectionEnum;
import com.howellyoung.exchange.enums.OrderStatusEnum;
import com.howellyoung.exchange.order.entity.OrderEntity;
import org.springframework.stereotype.Component;


@Component
public class MatchingService {

    public final OrderBook bidBook = new OrderBook(OrderDirectionEnum.BID);
    public final OrderBook askBook = new OrderBook(OrderDirectionEnum.ASK);
    public BigDecimal marketPrice = BigDecimal.ZERO; // 最新市场价
    private long sequenceId; // MatchingService State, using the processing order's sequenceId

    public MatchingResult processOrder(OrderEntity order) {
        return switch (order.direction) {
            case BID -> processOrder(order, this.askBook, this.bidBook);
            case ASK -> processOrder(order, this.bidBook, this.askBook);
            default -> throw new IllegalArgumentException("Invalid direction.");
        };
    }

    /**
     * @param takerOrder  要处理的订单
     * @param makerBook   尝试匹配成交的OrderBook
     * @param anotherBook 未能完全成交后挂单的OrderBook
     * @return 成交结果
     */
    private MatchingResult processOrder(OrderEntity takerOrder, OrderBook makerBook,
                                        OrderBook anotherBook) {
        this.sequenceId = takerOrder.sequenceId;
        long timeStamp = takerOrder.createdAt;
        MatchingResult matchingResult = new MatchingResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;
        //处理takerOrder
        for (; ; ) {
            OrderEntity makerOrder = makerBook.getFirst();
            if (makerOrder == null) {
                // 对手盘不存在:
                break;
            }
            if (takerOrder.direction == OrderDirectionEnum.BID && takerOrder.price.compareTo(makerOrder.price) < 0) {
                // 买单出价<卖盘最低出价，无法成交:
                break;
            } else if (takerOrder.direction == OrderDirectionEnum.ASK && takerOrder.price.compareTo(makerOrder.price) > 0) {
                // 卖单出价>买盘最高出价，无法成交:
                break;
            }
            // 以Maker价格成交:
            this.marketPrice = makerOrder.price;
            // 待成交数量为两者较小值:
            BigDecimal matchedQuantity = takerUnfilledQuantity.min(makerOrder.unfilledQuantity);
            // 成交记录:
            matchingResult.add(makerOrder.price, matchedQuantity, makerOrder);
            // 更新成交后的订单数量:
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchedQuantity);
            BigDecimal makerUnfilledQuantity = makerOrder.unfilledQuantity.subtract(matchedQuantity);
            // 对手盘完全成交后，从订单簿中删除:
            if (makerUnfilledQuantity.signum() == 0) {
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatusEnum.FULLY_FILLED, timeStamp);
                makerBook.remove(makerOrder);
            } else {
                // 对手盘部分成交:
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatusEnum.PARTIAL_FILLED, timeStamp);
            }
            // Taker订单完全成交后，退出循环:
            if (takerUnfilledQuantity.signum() == 0) {
                takerOrder.updateOrder(takerUnfilledQuantity, OrderStatusEnum.FULLY_FILLED, timeStamp);
                break;
            }
        }
        // Taker订单未完全成交时，放入订单簿:
        if (takerUnfilledQuantity.signum() > 0) {
            takerOrder.updateOrder(takerUnfilledQuantity,
                    takerUnfilledQuantity.compareTo(takerOrder.quantity) == 0 ? OrderStatusEnum.PENDING
                            : OrderStatusEnum.PARTIAL_FILLED,
                    timeStamp);
            anotherBook.add(takerOrder);
        }
        return matchingResult;
    }

    public void cancel(long ts, OrderEntity order) {
        OrderBook book = order.direction == OrderDirectionEnum.BID ? this.bidBook : this.askBook;
        if (!book.remove(order)) {
            throw new IllegalArgumentException("Order not found in order book.");
        }
        OrderStatusEnum status = order.unfilledQuantity.compareTo(order.quantity) == 0 ? OrderStatusEnum.FULLY_CANCELLED
                : OrderStatusEnum.PARTIAL_CANCELLED;
        order.updateOrder(order.unfilledQuantity, status, ts);
    }

    public OrderBookBean getOrderBook(int maxDepth) {
        return new OrderBookBean(this.sequenceId, this.marketPrice, this.bidBook.getOrderBook(maxDepth),
                this.askBook.getOrderBook(maxDepth));
    }

    public void debug() {
        System.out.println("---------- match engine ----------");
        System.out.println(this.askBook);
        System.out.println("  ----------");
        System.out.println("  " + this.marketPrice);
        System.out.println("  ----------");
        System.out.println(this.bidBook);
        System.out.println("---------- // match engine ----------");
    }
}
