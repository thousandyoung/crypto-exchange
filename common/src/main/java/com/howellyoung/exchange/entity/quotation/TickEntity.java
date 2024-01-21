package com.howellyoung.exchange.entity.quotation;

import java.math.BigDecimal;

import com.howellyoung.exchange.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ticks", uniqueConstraints = @UniqueConstraint(name = "UNI_T_M", columnNames = { "takerOrderId",
        "makerOrderId" }), indexes = @Index(name = "IDX_CAT", columnList = "createdAt"))
public class TickEntity implements BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public Long id;

    @Column(nullable = false, updatable = false)
    public Long sequenceId;

    @Column(nullable = false, updatable = false)
    public Long takerOrderId;

    @Column(nullable = false, updatable = false)
    public Long makerOrderId;

    /**
     * Bit for taker direction: 1=LONG, 0=SHORT.
     */
    @Column(nullable = false, updatable = false)
    public boolean takerDirection;

    @Column(nullable = false, updatable = false, precision = DECIMAL_PRECISION, scale = DECIMAL_SCALE)
    public BigDecimal price;

    @Column(nullable = false, updatable = false, precision = DECIMAL_PRECISION, scale = DECIMAL_SCALE)
    public BigDecimal quantity;

    /**
     * Created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public Long createdAt;

    public String toJson() {
        return "[" + createdAt + "," + (takerDirection ? 1 : 0) + "," + price + "," + quantity + "]";
    }
}
