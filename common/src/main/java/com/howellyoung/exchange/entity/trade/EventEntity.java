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
    public Long sequenceId;

    /**
     * Keep previous id. The previous id of first event is 0.
     */
    @Column(nullable = false, updatable = false)
    public Long previousId;

    /**
     * JSON-encoded event data.
     */
    @Column(nullable = false, updatable = false, length = CHAR_LENGTH_10000)
    public String data;

    @Column(nullable = false, updatable = false)
    public Long createdAt;

    @Override
    public String toString() {
        return "EventEntity [sequenceId=" + sequenceId + ", previousId=" + previousId + ", data=" + data
                + ", createdAt=" + createdAt + "]";
    }
}
