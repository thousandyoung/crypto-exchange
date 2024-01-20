package com.howellyoung.exchange.controller;

import java.util.Map;

import com.howellyoung.exchange.bean.TransferRequestBean;
import com.howellyoung.exchange.enums.UserTypeEnum;
import com.howellyoung.exchange.message.event.TransferEvent;
import com.howellyoung.exchange.service.SendEventService;
import com.howellyoung.exchange.util.AbstractApiController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/*
    api used between modules
 */

@RestController
@RequestMapping("/internal")
class TradingInternalApiController extends AbstractApiController {

    private final SendEventService sendEventService;

    public TradingInternalApiController(SendEventService sendEventService) {
        this.sendEventService = sendEventService;
    }

    /**
     * 处理一个转账请求，可重复调用，重复发送消息，根据uniqueId去重，仅定序一次。
     */
    @PostMapping("/transfer")
    public Map<String, Boolean> transferIn(@RequestBody TransferRequestBean transferRequest) {
        logger.info("transfer request: transferId={}, fromUserId={}, toUserId={}, asset={}, amount={}",
                transferRequest.transferId, transferRequest.fromUserId, transferRequest.toUserId, transferRequest.asset,
                transferRequest.amount);
        transferRequest.validate();

        var message = new TransferEvent();
        // IMPORTANT: set uniqueId to make sure the message will be sequenced only once:
        message.uniqueId = transferRequest.transferId;
        message.fromUserId = transferRequest.fromUserId;
        message.toUserId = transferRequest.toUserId;
        message.asset = transferRequest.asset;
        message.amount = transferRequest.amount;
        message.sufficient = transferRequest.fromUserId.longValue() != UserTypeEnum.SYSTEM.getInternalUserId();
        this.sendEventService.sendMessage(message);
        logger.info("transfer event sent: {}", message);
        return Map.of("result", Boolean.TRUE);
    }
}
