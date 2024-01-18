package com.howellyoung.exchange.store;

import java.util.List;
import java.util.stream.Collectors;

import com.howellyoung.exchange.entity.trade.EventEntity;
import com.howellyoung.exchange.entity.trade.MatchingDetailEntity;
import com.howellyoung.exchange.entity.trade.OrderEntity;
import com.howellyoung.exchange.entity.base.BaseEntity;
import com.howellyoung.exchange.message.event.BaseEvent;
import com.howellyoung.exchange.messaging.MessageTypes;
import com.howellyoung.exchange.repository.EventRepository;
import com.howellyoung.exchange.repository.MatchingDetailRepository;
import com.howellyoung.exchange.repository.OrderRepository;
import com.howellyoung.exchange.util.LoggerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Component
@Transactional
public class StoreService extends LoggerBase {

    @Autowired
    MessageTypes messageTypes;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    private MatchingDetailRepository matchingDetailRepository;

    @Autowired
    private OrderRepository orderRepository;
    public List<BaseEvent> loadEventsFromDb(long lastEventId) {
        List<EventEntity> events = this.eventRepository.findBySequenceIdGreaterThanOrderByIdAsc(lastEventId, PageRequest.of(0, 100000));
        return events.stream().map(event -> (BaseEvent) messageTypes.deserialize(event.data))
                .collect(Collectors.toList());
    }

    @Transactional
    public void insertIgnore(List<? extends BaseEntity> list) {
        for (BaseEntity entity : list) {
            if (entity instanceof MatchingDetailEntity) {
                matchingDetailRepository.save((MatchingDetailEntity) entity);
            } else if (entity instanceof OrderEntity) {
                orderRepository.save((OrderEntity) entity);
            }
        }
    }
}