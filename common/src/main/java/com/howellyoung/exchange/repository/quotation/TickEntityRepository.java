package com.howellyoung.exchange.repository.quotation;

// TickEntityRepository.java
import com.howellyoung.exchange.entity.quotation.TickEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TickEntityRepository extends JpaRepository<TickEntity, Long> {

    @Modifying
    @Query(value = "insert ignore into ticks (sequenceId, takerOrderId, makerOrderId, takerDirection, price, quantity, createdAt) values (:#{#entity.sequenceId}, :#{#entity.takerOrderId}, :#{#entity.makerOrderId}, :#{#entity.takerDirection}, :#{#entity.price}, :#{#entity.quantity}, :#{#entity.createdAt})", nativeQuery = true)
    void saveIgnore(TickEntity entity);
}