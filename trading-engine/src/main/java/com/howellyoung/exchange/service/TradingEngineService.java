package com.howellyoung.exchange.service;

import com.howellyoung.exchange.assets.Asset;
import com.howellyoung.exchange.assets.AssetService;
import com.howellyoung.exchange.bean.OrderBookBean;
import com.howellyoung.exchange.clearing.ClearingService;
import com.howellyoung.exchange.entity.quotation.TickEntity;
import com.howellyoung.exchange.entity.trade.MatchingDetailEntity;
import com.howellyoung.exchange.entity.trade.OrderEntity;
import com.howellyoung.exchange.enums.AssetEnum;
import com.howellyoung.exchange.enums.MatchTypeEnum;
import com.howellyoung.exchange.enums.OrderDirectionEnum;
import com.howellyoung.exchange.enums.UserTypeEnum;
import com.howellyoung.exchange.matching.MatchingDetailRecord;
import com.howellyoung.exchange.matching.MatchingResult;
import com.howellyoung.exchange.matching.MatchingService;
import com.howellyoung.exchange.message.ApiResultMessage;
import com.howellyoung.exchange.message.NotificationMessage;
import com.howellyoung.exchange.message.TickMessage;
import com.howellyoung.exchange.message.event.BaseEvent;
import com.howellyoung.exchange.message.event.OrderCancelEvent;
import com.howellyoung.exchange.message.event.OrderRequestEvent;
import com.howellyoung.exchange.message.event.TransferEvent;
import com.howellyoung.exchange.messaging.MessageProducer;
import com.howellyoung.exchange.messaging.MessageConsumer;
import com.howellyoung.exchange.messaging.Messaging;
import com.howellyoung.exchange.messaging.MessagingFactory;
import com.howellyoung.exchange.order.OrderService;
import com.howellyoung.exchange.redis.RedisCache;
import com.howellyoung.exchange.redis.RedisService;
import com.howellyoung.exchange.store.StoreService;
import com.howellyoung.exchange.util.IpUtil;
import com.howellyoung.exchange.util.JsonUtil;
import com.howellyoung.exchange.util.LoggerBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class TradingEngineService extends LoggerBase {
    // injection values
    private ZoneId zoneId;
    private int orderBookDepth;
    private boolean debugMode;
    private final AssetService assetService;
    private final OrderService orderService;
    public final MatchingService matchingService;
    private final ClearingService clearingService;
    private final MessagingFactory messagingFactory;
    private final StoreService storeService;
    private final RedisService redisService;
    // error mark
    private boolean fatalError = false;
    //kafka
    private MessageConsumer consumer;
    private MessageProducer<TickMessage> producer;
    private long lastEventSequenceId = 0;
    //maintain latest orderBook
    private boolean isOrderBookUpdated = false;
    //redis script
    private String shaUpdateOrderBookLua;

    private Thread tickThread;
    private Thread notifyThread;
    private Thread apiResultThread;
    private Thread orderBookThread;
    private Thread dbThread;

    // persistence
    private OrderBookBean latestOrderBook = null;
    private Queue<List<OrderEntity>> orderQueue = new ConcurrentLinkedQueue<>();
    private Queue<List<MatchingDetailEntity>> matchQueue = new ConcurrentLinkedQueue<>();
    // kafka messaging, export tick msg
    private Queue<TickMessage> tickQueue = new ConcurrentLinkedQueue<>();
    // redis messaging, export notification, api result
    private Queue<ApiResultMessage> apiResultQueue = new ConcurrentLinkedQueue<>();
    private Queue<NotificationMessage> notificationQueue = new ConcurrentLinkedQueue<>();

    public TradingEngineService(
            ZoneId zoneId,
            @Value("#{exchangeConfiguration.orderBookDepth ?: 100}") int orderBookDepth,
            @Value("#{exchangeConfiguration.debugMode ?: false}") boolean debugMode,
            AssetService assetService,
            OrderService orderService,
            MatchingService matchingService,
            ClearingService clearingService,
            MessagingFactory messagingFactory,
            StoreService storeService,
            RedisService redisService
    ) {
        this.zoneId = zoneId == null ? ZoneId.systemDefault() : zoneId;
        this.orderBookDepth = orderBookDepth;
        this.debugMode = debugMode;
        this.assetService = assetService;
        this.orderService = orderService;
        this.matchingService = matchingService;
        this.clearingService = clearingService;
        this.messagingFactory = messagingFactory;
        this.storeService = storeService;
        this.redisService = redisService;
    }
    @PostConstruct
    public void init() {
        this.shaUpdateOrderBookLua = this.redisService.loadScriptFromClassPath("/redis/update-orderbook.lua");
        this.consumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.TRADE, IpUtil.getHostId(),
                this::processMessages);
        this.producer = this.messagingFactory.createMessageProducer(Messaging.Topic.TICK, TickMessage.class);
        this.tickThread = new Thread(this::runTickThread, "async-tick");
        this.tickThread.start();
        this.notifyThread = new Thread(this::runNotifyThread, "async-notify");
        this.notifyThread.start();
        this.orderBookThread = new Thread(this::runOrderBookThread, "async-orderbook");
        this.orderBookThread.start();
        this.apiResultThread = new Thread(this::runApiResultThread, "async-api-result");
        this.apiResultThread.start();
        this.dbThread = new Thread(this::runDbThread, "async-db");
        this.dbThread.start();
    }

    @PreDestroy
    public void destroy() {
        this.consumer.stop();
        this.orderBookThread.interrupt();
        this.dbThread.interrupt();
    }

    private void runTickThread() {
        logger.info("start tick thread...");
        for (; ; ) {
            List<TickMessage> msgs = new ArrayList<>();
            for (; ; ) {
                TickMessage msg = tickQueue.poll();
                if (msg != null) {
                    msgs.add(msg);
                    if (msgs.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (!msgs.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("send {} tick messages...", msgs.size());
                }
                this.producer.sendMessages(msgs);//kafka
            } else {
                // 无TickMessage时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    private void runNotifyThread() {
        logger.info("start publish notify to redis...");
        for (; ; ) {
            NotificationMessage msg = this.notificationQueue.poll();
            if (msg != null) {
                redisService.publish(RedisCache.Topic.NOTIFICATION, JsonUtil.convertObjectToJsonString(msg));
            } else {
                // 无推送时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    private void runApiResultThread() {
        logger.info("start publish api result to redis...");
        for (; ; ) {
            ApiResultMessage result = this.apiResultQueue.poll();
            if (result != null) {
                redisService.publish(RedisCache.Topic.TRADING_API_RESULT, JsonUtil.convertObjectToJsonString(result));
            } else {
                // 无推送时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    private void runOrderBookThread() {
        logger.info("start update orderbook snapshot to redis...");
        long lastSequenceId = 0;
        for (; ; ) {
            // 获取OrderBookBean的引用，确保后续操作针对局部变量而非成员变量:
            final OrderBookBean orderBook = this.latestOrderBook;
            // 仅在OrderBookBean更新后刷新Redis:
            if (orderBook != null && orderBook.sequenceId > lastSequenceId) {
                if (logger.isDebugEnabled()) {
                    logger.debug("update orderbook snapshot at sequence id {}...", orderBook.sequenceId);
                }
                redisService.executeScriptReturnBoolean(this.shaUpdateOrderBookLua,
                        // keys: [cache-key]
                        new String[]{RedisCache.Key.ORDER_BOOK},
                        // args: [sequenceId, json-data]
                        new String[]{String.valueOf(orderBook.sequenceId), JsonUtil.convertObjectToJsonString(orderBook)});
                lastSequenceId = orderBook.sequenceId;
            } else {
                // 无更新时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    private void runDbThread() {
        logger.info("start batch insert to db...");
        for (; ; ) {
            try {
                saveToDb();
            } catch (InterruptedException e) {
                logger.warn("{} was interrupted.", Thread.currentThread().getName());
                break;
            }
        }
    }

    // for recovering, called by dbExecutor thread only:
    private void saveToDb() throws InterruptedException {
        if (!matchQueue.isEmpty()) {
            List<MatchingDetailEntity> batch = new ArrayList<>(1000);
            for (; ; ) {
                List<MatchingDetailEntity> matches = matchQueue.poll();
                if (matches != null) {
                    batch.addAll(matches);
                    if (batch.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            batch.sort(MatchingDetailEntity::compareTo);
            if (logger.isDebugEnabled()) {
                logger.debug("batch insert {} match details...", batch.size());
            }
            this.storeService.insertIgnore(batch);
        }
        if (!orderQueue.isEmpty()) {
            List<OrderEntity> batch = new ArrayList<>(1000);
            for (; ; ) {
                List<OrderEntity> orders = orderQueue.poll();
                if (orders != null) {
                    batch.addAll(orders);
                    if (batch.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            batch.sort(OrderEntity::compareTo);
            if (logger.isDebugEnabled()) {
                logger.debug("batch insert {} orders...", batch.size());
            }
            this.storeService.insertIgnore(batch);
        }
        if (matchQueue.isEmpty()) {
            Thread.sleep(1);
        }
    }

    private void markOrderBookAsUpdated() {
        this.isOrderBookUpdated = true;
    }

    private void resetOrderBookUpdateStatus() {
        this.isOrderBookUpdated = false;
    }

    private void updateOrderBookIfChanged() {
        if (this.isOrderBookUpdated) {
            // 获取最新的OrderBook快照:
            this.latestOrderBook = this.matchingService.getOrderBook(this.orderBookDepth);
            this.isOrderBookUpdated = false;
        }
    }

    public void processMessages(List<BaseEvent> messages) {
        resetOrderBookUpdateStatus();
        for (BaseEvent message : messages) {
            processEvent(message);
        }
        updateOrderBookIfChanged();
    }

    public void processEvent(BaseEvent event) {
        if (this.fatalError) {
            return;
        }
        if (event.sequenceId <= this.lastEventSequenceId) {
            logger.warn("skip duplicate event: {}", event);
            return;
        }
        if (event.previousId > this.lastEventSequenceId) {
            logger.warn("event lost: expected previous id {} but actual {} for event {}", this.lastEventSequenceId,
                    event.previousId, event);
            List<BaseEvent> events = this.storeService.loadEventsFromDb(this.lastEventSequenceId);
            if (events.isEmpty()) {
                logger.error("cannot load lost event from db.");
                panic();
                return;
            }
            for (BaseEvent e : events) {
                this.processEvent(e);
            }
            return;
        }
        if (event.previousId != lastEventSequenceId) {
            logger.error("bad event: expected previous id {} but actual {} for event: {}", this.lastEventSequenceId,
                    event.previousId, event);
            panic();
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("process event {} -> {}: {}...", this.lastEventSequenceId, event.sequenceId, event);
        }
        try {
            if (event instanceof OrderRequestEvent) {
                createOrder((OrderRequestEvent) event);
            } else if (event instanceof OrderCancelEvent) {
                cancelOrder((OrderCancelEvent) event);
            } else if (event instanceof TransferEvent) {
                TransferEvent transferEvent = (TransferEvent) event;
                transfer((TransferEvent) event);
            } else {
                logger.error("unable to process event type: {}", event.getClass().getName());
                panic();
                return;
            }
        } catch (Exception e) {
            logger.error("process event error.", e);
            panic();
            return;
        }
        this.lastEventSequenceId = event.sequenceId;
        if (logger.isDebugEnabled()) {
            logger.debug("set last processed sequence id: {}...", this.lastEventSequenceId);
        }
        if (debugMode) {
            this.validate();
            this.debug();
        }
    }

    private void panic() {
        logger.error("application panic. exit now...");
        this.fatalError = true;
        System.exit(1);
    }

    void transfer(TransferEvent event) {
        this.assetService.transferAvailableToAvailable(event.fromUserId, event.toUserId, event.asset, event.amount, event.sufficient);
    }

    void createOrder(OrderRequestEvent event) {
        ZonedDateTime zdt = Instant.ofEpochMilli(event.createdAt).atZone(zoneId);
        int year = zdt.getYear();
        int month = zdt.getMonth().getValue();
        long orderId = event.sequenceId * 10000 + (year * 100 + month);
        OrderEntity order = this.orderService.createOrder(event.sequenceId, event.createdAt, orderId, event.userId,
                event.direction, event.price, event.quantity);
        if (order == null) {
            logger.warn("create order failed.");
            // 推送失败结果:
            this.apiResultQueue.add(ApiResultMessage.createOrderFailed(event.refId, event.createdAt));
            return;
        }
        MatchingResult result = this.matchingService.processOrder(order);
        this.clearingService.clearMatchingResult(result);
        // 推送成功结果,注意必须复制一份OrderEntity,因为将异步序列化:
        this.apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, order.copy(), event.createdAt));
        markOrderBookAsUpdated();
        // 收集Notification:
        List<NotificationMessage> notifications = new ArrayList<>();
        notifications.add(createNotification(event.createdAt, "order_matched", order.userId, order.copy()));
        // 收集已完成的OrderEntity并生成MatchingDetailEntity, TickEntity:
        if (!result.matchingDetails.isEmpty()) {
            List<OrderEntity> closedOrders = new ArrayList<>();
            List<MatchingDetailEntity> matchingDetails = new ArrayList<>();
            List<TickEntity> ticks = new ArrayList<>();
            if (result.takerOrder.status.isFinalStatus) {
                closedOrders.add(result.takerOrder);
            }
            for (MatchingDetailRecord detail : result.matchingDetails) {
                OrderEntity maker = detail.makerOrder();
                notifications.add(createNotification(event.createdAt, "order_matched", maker.userId, maker.copy()));
                if (maker.status.isFinalStatus) {
                    closedOrders.add(maker);
                }
                MatchingDetailEntity takerDetail = generateMatchingDetailEntity(event.sequenceId, event.createdAt, detail,
                        true);
                MatchingDetailEntity makerDetail = generateMatchingDetailEntity(event.sequenceId, event.createdAt, detail,
                        false);
                matchingDetails.add(takerDetail);
                matchingDetails.add(makerDetail);
                TickEntity tick = new TickEntity();
                tick.sequenceId = event.sequenceId;
                tick.takerOrderId = detail.takerOrder().id;
                tick.makerOrderId = detail.makerOrder().id;
                tick.price = detail.price();
                tick.quantity = detail.quantity();
                tick.takerDirection = detail.takerOrder().direction == OrderDirectionEnum.BID;
                tick.createdAt = event.createdAt;
                ticks.add(tick);
            }
            // 异步写入数据库:
            this.orderQueue.add(closedOrders);
            this.matchQueue.add(matchingDetails);
            // 异步发送Tick消息:
            TickMessage msg = new TickMessage();
            msg.sequenceId = event.sequenceId;
            msg.createdAt = event.createdAt;
            msg.ticks = ticks;
            this.tickQueue.add(msg);
            // 异步通知OrderMatch:
            this.notificationQueue.addAll(notifications);
        }
    }

    private NotificationMessage createNotification(long ts, String type, Long userId, Object data) {
        NotificationMessage msg = new NotificationMessage();
        msg.createdAt = ts;
        msg.type = type;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }

    MatchingDetailEntity generateMatchingDetailEntity(long sequenceId, long timestamp, MatchingDetailRecord detail,
                                                      boolean forTaker) {
        MatchingDetailEntity d = new MatchingDetailEntity();
        d.sequenceId = sequenceId;
        d.orderId = forTaker ? detail.takerOrder().id : detail.makerOrder().id;
        d.counterOrderId = forTaker ? detail.makerOrder().id : detail.takerOrder().id;
        d.direction = forTaker ? detail.takerOrder().direction : detail.makerOrder().direction;
        d.price = detail.price();
        d.quantity = detail.quantity();
        d.type = forTaker ? MatchTypeEnum.TAKER : MatchTypeEnum.MAKER;
        d.userId = forTaker ? detail.takerOrder().userId : detail.makerOrder().userId;
        d.counterUserId = forTaker ? detail.makerOrder().userId : detail.takerOrder().userId;
        d.createdAt = timestamp;
        return d;
    }

    void cancelOrder(OrderCancelEvent event) {
        OrderEntity order = this.orderService.getOrder(event.refOrderId);
        // 未找到活动订单或订单不属于该用户:
        if (order == null || order.userId.longValue() != event.userId.longValue()) {
            // 发送失败消息:
            this.apiResultQueue.add(ApiResultMessage.cancelOrderFailed(event.refId, event.createdAt));
            return;
        }
        this.matchingService.cancel(event.createdAt, order);
        this.clearingService.clearCancelOrder(order);
        markOrderBookAsUpdated();
        // 发送成功消息:
        this.apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, order, event.createdAt));
        this.notificationQueue.add(createNotification(event.createdAt, "order_canceled", order.userId, order));
    }

    public void debug() {
        System.out.println("========== trading engine ==========");
        this.assetService.debug();
        this.orderService.debug();
        this.matchingService.debug();
        System.out.println("========== // trading engine ==========");
    }

    public void validate() {
        logger.debug("start validate...");
        validateAssets();
        validateOrdersAndAsset();
        validateMatchingAndOrder();
        logger.debug("validate ok.");
    }

    void validateAssets() {
        // 验证系统资产完整性:
        BigDecimal totalUSD = BigDecimal.ZERO;
        BigDecimal totalBTC = BigDecimal.ZERO;
        for (Entry<Long, Map<AssetEnum, Asset>> userAssetEntry : this.assetService.getAllUserAssetsMap().entrySet()) {
            Long userId = userAssetEntry.getKey();
            Map<AssetEnum, Asset> assets = userAssetEntry.getValue();
            for (Entry<AssetEnum, Asset> entry : assets.entrySet()) {
                AssetEnum assetId = entry.getKey();
                Asset asset = entry.getValue();
                if (userId.longValue() == UserTypeEnum.SYSTEM.getInternalUserId()) {
                    // 系统负债账户available不允许为正:
                    require(asset.getAvailable().signum() <= 0, "Debt has positive available: " + asset);
                    // 系统负债账户frozen必须为0:
                    require(asset.getFrozen().signum() == 0, "Debt has non-zero frozen: " + asset);
                } else {
                    // 交易用户的available/frozen不允许为负数:
                    require(asset.getAvailable().signum() >= 0, "Trader has negative available: " + asset);
                    require(asset.getFrozen().signum() >= 0, "Trader has negative frozen: " + asset);
                }
                switch (assetId) {
                    case USD -> {
                        totalUSD = totalUSD.add(asset.getTotal());
                    }
                    case BTC -> {
                        totalBTC = totalBTC.add(asset.getTotal());
                    }
                    default -> require(false, "Unexpected asset id: " + assetId);
                }
            }
        }
        // 各类别资产总额为0:
        require(totalUSD.signum() == 0, "Non zero USD balance: " + totalUSD);
        require(totalBTC.signum() == 0, "Non zero BTC balance: " + totalBTC);
    }

    void validateOrdersAndAsset() {
        // 验证订单:
        Map<Long, Map<AssetEnum, BigDecimal>> userOrderFrozen = new HashMap<>();
        for (Entry<Long, OrderEntity> entry : this.orderService.getAllActiveOrdersMap().entrySet()) {
            OrderEntity order = entry.getValue();
            require(order.unfilledQuantity.signum() > 0, "Active order must have positive unfilled amount: " + order);
            switch (order.direction) {
                case BID -> {
                    // 订单必须在MatchingService中:
                    require(this.matchingService.bidBook.exist(order), "order not found in buy book: " + order);
                    // 累计冻结的USD:
                    userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                    Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                    frozenAssets.putIfAbsent(AssetEnum.USD, BigDecimal.ZERO);
                    BigDecimal frozen = frozenAssets.get(AssetEnum.USD);
                    frozenAssets.put(AssetEnum.USD, frozen.add(order.price.multiply(order.unfilledQuantity)));
                }
                case ASK -> {
                    // 订单必须在MatchingService中:
                    require(this.matchingService.askBook.exist(order), "order not found in sell book: " + order);
                    // 累计冻结的BTC:
                    userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                    Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                    frozenAssets.putIfAbsent(AssetEnum.BTC, BigDecimal.ZERO);
                    BigDecimal frozen = frozenAssets.get(AssetEnum.BTC);
                    frozenAssets.put(AssetEnum.BTC, frozen.add(order.unfilledQuantity));
                }
                default -> require(false, "Unexpected order direction: " + order.direction);
            }
        }
        // 订单冻结的累计金额必须和Asset冻结一致:
        for (Entry<Long, Map<AssetEnum, Asset>> userEntry : this.assetService.getAllUserAssetsMap().entrySet()) {
            Long userId = userEntry.getKey();
            Map<AssetEnum, Asset> assets = userEntry.getValue();
            for (Entry<AssetEnum, Asset> entry : assets.entrySet()) {
                AssetEnum assetId = entry.getKey();
                Asset asset = entry.getValue();
                if (asset.getFrozen().signum() > 0) {
                    Map<AssetEnum, BigDecimal> orderFrozen = userOrderFrozen.get(userId);
                    require(orderFrozen != null, "No order frozen found for user: " + userId + ", asset: " + asset);
                    BigDecimal frozen = orderFrozen.get(assetId);
                    require(frozen != null, "No order frozen found for asset: " + asset);
                    require(frozen.compareTo(asset.getFrozen()) == 0,
                            "Order frozen " + frozen + " is not equals to asset frozen: " + asset);
                    // 从userOrderFrozen中删除已验证的Asset数据:
                    orderFrozen.remove(assetId);
                }
            }
        }
        // userOrderFrozen不存在未验证的Asset数据:
        for (Entry<Long, Map<AssetEnum, BigDecimal>> userEntry : userOrderFrozen.entrySet()) {
            Long userId = userEntry.getKey();
            Map<AssetEnum, BigDecimal> frozenAssets = userEntry.getValue();
            require(frozenAssets.isEmpty(), "User " + userId + " has unexpected frozen for order: " + frozenAssets);
        }
    }

    void validateMatchingAndOrder() {
        // OrderBook的Order必须在ActiveOrders中:
        Map<Long, OrderEntity> copyOfActiveOrders = new HashMap<>(this.orderService.getAllActiveOrdersMap());
        for (OrderEntity order : this.matchingService.bidBook.orderMap.values()) {
            require(copyOfActiveOrders.remove(order.id) == order,
                    "Order in buy book is not in active orders: " + order);
        }
        for (OrderEntity order : this.matchingService.askBook.orderMap.values()) {
            require(copyOfActiveOrders.remove(order.id) == order,
                    "Order in sell book is not in active orders: " + order);
        }
        // orderService的所有Order必须在matchingService中:
        require(copyOfActiveOrders.isEmpty(), "Not all active orders are in order book.");
    }

    void require(boolean condition, String errorMessage) {
        if (!condition) {
            logger.error("validate failed: {}", errorMessage);
            panic();
        }
    }
}
