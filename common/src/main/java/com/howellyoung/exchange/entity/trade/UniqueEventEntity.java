package com.howellyoung.exchange.entity.trade;

import com.howellyoung.exchange.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// for persistence, must be unique event, because upstream may send duplicate events
@Entity
@Table(name = "unique_events")
public class UniqueEventEntity implements BaseEntity {

    @Id
    @Column(nullable = false, updatable = false, length = CHAR_LENGTH_50)
    public String uniqueId;

    /**
     * Which event associated.
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * Created time (milliseconds). Set after sequenced.
     */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public String toString() {
        return "UniqueEventEntity [uniqueId=" + uniqueId + ", sequenceId=" + sequenceId + ", createdAt=" + createdAt
                + "]";
    }
}
