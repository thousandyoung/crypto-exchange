package com.howellyoung.exchange.entity.trade;

import com.howellyoung.exchange.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;



/**
 * Event readonly entity.
 */
@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(name = "UNI_PREV_ID", columnNames = { "previousId" }))
public class EventEntity implements BaseEntity {

    /**
     * Primary key: assigned.
     */
    @Id
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * Keep previous id. The previous id of first event is 0.
     * // previousId is used to ensure the order of event processing.
     * // It should be the sequenceId of the previous event.
     * // If the previousId is greater than the last processed sequenceId, it means some events are lost and we need to load them from the database.
     * // If the previousId is not equal to the last processed sequenceId, it means the order of events is incorrect, and we need to stop processing.
     */
    @Column(nullable = false, updatable = false)
    public long previousId;

    /**
     * JSON-encoded event data.
     */
    @Column(nullable = false, updatable = false, length = CHAR_LENGTH_1000)
    public String data;

    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public String toString() {
        return "EventEntity [sequenceId=" + sequenceId + ", previousId=" + previousId + ", data=" + data
                + ", createdAt=" + createdAt + "]";
    }
}
