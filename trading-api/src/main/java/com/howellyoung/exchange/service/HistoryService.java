package com.howellyoung.exchange.service;

import java.util.List;
import java.util.stream.Collectors;

import com.howellyoung.exchange.bean.SimpleMatchingDetailRecord;
import com.howellyoung.exchange.entity.trade.MatchingDetailEntity;
import com.howellyoung.exchange.entity.trade.OrderEntity;
import com.howellyoung.exchange.repository.MatchingDetailRepository;
import com.howellyoung.exchange.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class HistoryService {

    private final OrderRepository orderRepository;
    private final MatchingDetailRepository matchingDetailRepository;

    public HistoryService(OrderRepository orderRepository, MatchingDetailRepository matchingDetailRepository) {
        this.orderRepository = orderRepository;
        this.matchingDetailRepository = matchingDetailRepository;
    }

    public List<OrderEntity> getHistoryOrders(Long userId, int maxResults) {
        return orderRepository.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, maxResults));
    }

    public OrderEntity getHistoryOrder(Long userId, Long orderId) {
        return (OrderEntity) orderRepository.findByIdAndUserId(orderId, userId).orElse(null);
    }

    public List<SimpleMatchingDetailRecord> getHistoryMatchingDetails(Long orderId) {
        List<MatchingDetailEntity> details = matchingDetailRepository.findByOrderIdOrderById(orderId);
        return details.stream().map(e -> new SimpleMatchingDetailRecord(e.price, e.quantity, e.type))
                .collect(Collectors.toList());
    }
}
