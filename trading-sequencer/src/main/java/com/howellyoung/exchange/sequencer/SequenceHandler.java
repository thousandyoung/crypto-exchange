package com.howellyoung.exchange.sequencer;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import com.howellyoung.exchange.entity.trade.EventEntity;
import com.howellyoung.exchange.entity.trade.UniqueEventEntity;
import com.howellyoung.exchange.message.event.BaseEvent;
import com.howellyoung.exchange.messaging.MessageTypes;
import com.howellyoung.exchange.repository.trade.EventRepository;
import com.howellyoung.exchange.repository.trade.UniqueEventRepository;
import com.howellyoung.exchange.util.LoggerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * Process events as batch.
 */
@Component
@Transactional(rollbackFor = Throwable.class)
public class SequenceHandler extends LoggerBase {
    private long lastTimestamp = 0;
    private final UniqueEventRepository uniqueEventRepository;
    private final EventRepository eventRepository;

    @Autowired
    public SequenceHandler(UniqueEventRepository uniqueEventRepository, EventRepository eventRepository) {
        this.uniqueEventRepository = uniqueEventRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Set sequence for each message, persist into database as batch.
     * 
     * @return Sequenced messages.
     */
    public List<BaseEvent> sequenceMessages(final MessageTypes messageTypes, final AtomicLong sequence,
                                            final List<BaseEvent> messages) throws Exception {
        final long t = System.currentTimeMillis();
        if (t < this.lastTimestamp) {
            logger.warn("[Sequence] current time {} is turned back from {}!", t, this.lastTimestamp);
        } else {
            this.lastTimestamp = t;
        }
        List<UniqueEventEntity> uniques = null;
        Set<String> uniqueKeys = null;
        List<BaseEvent> sequencedMessages = new ArrayList<>(messages.size());
        List<EventEntity> events = new ArrayList<>(messages.size());
        for (BaseEvent message : messages) {
            UniqueEventEntity unique = null;
            final String uniqueId = message.uniqueId;
            // check uniqueId:
            if (uniqueId != null) {
                if ((uniqueKeys != null && uniqueKeys.contains(uniqueId))
                        || uniqueEventRepository.findById(uniqueId).isPresent()) {
                    logger.warn("ignore processed unique message: {}", message);
                    continue;
                }
                unique = new UniqueEventEntity();
                unique.uniqueId = uniqueId;
                unique.createdAt = message.createdAt;
                if (uniques == null) {
                    uniques = new ArrayList<>();
                }
                uniques.add(unique);
                if (uniqueKeys == null) {
                    uniqueKeys = new HashSet<>();
                }
                uniqueKeys.add(uniqueId);
                logger.info("unique event {} sequenced.", uniqueId);
            }

            final long previousId = sequence.get();
            final long currentId = sequence.incrementAndGet();

            // 先设置message的sequenceId / previouseId / createdAt，再序列化并落库:
            message.sequenceId = currentId;
            message.previousId = previousId;
            message.createdAt = this.lastTimestamp;

            // 如果此消息关联了UniqueEvent，给UniqueEvent加上相同的sequenceId：
            if (unique != null) {
                unique.sequenceId = message.sequenceId;
            }

            // create baseEvent and save to db later:
            EventEntity event = new EventEntity();
            event.previousId = previousId;
            event.sequenceId = currentId;

            event.data = messageTypes.serialize(message);
            event.createdAt = this.lastTimestamp; // same as message.createdAt
            events.add(event);
            // will send later:
            sequencedMessages.add(message);
        }

        if (uniques != null) {
            uniqueEventRepository.saveAll(uniques);
        }
        eventRepository.saveAll(events);
        return sequencedMessages;
    }

    public long getMaxSequenceId() {
        Optional<EventEntity> last = eventRepository.findTopByOrderBySequenceIdDesc();
        if (!last.isPresent()) {
            logger.info("no max sequenceId found. set max sequenceId = 0.");
            return 0;
        }
        this.lastTimestamp = last.get().createdAt;
        logger.info("find max sequenceId = {}, last timestamp = {}", last.get().sequenceId, this.lastTimestamp);
        return last.get().sequenceId;
    }
}
