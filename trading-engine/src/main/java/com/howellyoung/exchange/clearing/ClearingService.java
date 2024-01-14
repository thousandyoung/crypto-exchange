package com.howellyoung.exchange.clearing;

import java.math.BigDecimal;

import com.howellyoung.exchange.assets.AssetService;
import com.howellyoung.exchange.enums.AssetEnum;
import com.howellyoung.exchange.matching.MatchingDetailRecord;
import com.howellyoung.exchange.matching.MatchingResult;
import com.howellyoung.exchange.entity.OrderEntity;
import com.howellyoung.exchange.order.OrderService;
import com.howellyoung.exchange.util.LoggerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ClearingService extends LoggerBase {

    final AssetService assetService;

    final OrderService orderService;

    public ClearingService(@Autowired AssetService assetService, @Autowired OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    public void clearMatchingResult(MatchingResult result) {
        OrderEntity taker = result.takerOrder;
        switch (taker.direction) {
            case BID -> {
                // 买入时，按Maker的价格成交：
                for (MatchingDetailRecord matchingDetail : result.matchingDetails) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "clear buy matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                                matchingDetail.price(), matchingDetail.quantity(), matchingDetail.takerOrder().id, matchingDetail.makerOrder().id,
                                matchingDetail.takerOrder().userId, matchingDetail.makerOrder().userId);
                    }
                    OrderEntity maker = matchingDetail.makerOrder();
                    BigDecimal matchedQuantity = matchingDetail.quantity();
                    BigDecimal matchedTradeValue = maker.price.multiply(matchedQuantity);
                    if (taker.price.compareTo(maker.price) > 0) {
                        // 成交价<Bid Price，被freeze的差价USD退回available:
                        BigDecimal unfreezeFunds = taker.price.multiply(matchedQuantity).subtract(matchedTradeValue);
                        logger.debug("unfree extra unused quote {} back to taker user {}", unfreezeFunds, taker.userId);
                        assetService.unfreeze(taker.userId, AssetEnum.USD, unfreezeFunds);
                    }
                    // 买方USD转入卖方账户:
                    assetService.transferFrozenToAvailable(taker.userId, maker.userId, AssetEnum.USD, matchedTradeValue, true);
                    // 卖方BTC转入买方账户:
                    assetService.transferFrozenToAvailable(maker.userId, taker.userId, AssetEnum.BTC, matchedQuantity, true);
                    // 删除完全成交的Maker:
                    orderService.processCompletedOrder(maker);
                }
                // 删除完全成交的Taker:
                orderService.processCompletedOrder(taker);
            }
            case ASK -> {
                for (MatchingDetailRecord matchingDetail : result.matchingDetails) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "clear sell matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                                matchingDetail.price(), matchingDetail.quantity(), matchingDetail.takerOrder().id, matchingDetail.makerOrder().id,
                                matchingDetail.takerOrder().userId, matchingDetail.makerOrder().userId);
                    }
                    OrderEntity maker = matchingDetail.makerOrder();
                    BigDecimal matchedQuantity = matchingDetail.quantity();
                    BigDecimal matchedTradeValue = maker.price.multiply(matchedQuantity);
                    // 卖方BTC转入买方账户:
                    assetService.transferFrozenToAvailable(taker.userId, maker.userId, AssetEnum.BTC, matchedQuantity, true);
                    // 买方USD转入卖方账户:
                    assetService.transferFrozenToAvailable(maker.userId, taker.userId, AssetEnum.USD, matchedTradeValue, true);
                    // 删除完全成交的Maker:
                    orderService.processCompletedOrder(maker);
                }
                // 删除完全成交的Taker:
                orderService.processCompletedOrder(taker);
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
    }

    public void clearCancelOrder(OrderEntity order) {
        switch (order.direction) {
            case BID -> {
                // 解冻USD = 价格 x 未成交数量
                assetService.unfreeze(order.userId, AssetEnum.USD, order.price.multiply(order.unfilledQuantity));
            }
            case ASK -> {
                // 解冻BTC = 未成交数量
                assetService.unfreeze(order.userId, AssetEnum.BTC, order.unfilledQuantity);
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
        // 从OrderService中删除订单:
        orderService.removeOrder(order.id);
    }
}
