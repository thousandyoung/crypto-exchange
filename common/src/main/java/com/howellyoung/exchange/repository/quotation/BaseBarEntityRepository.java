package com.howellyoung.exchange.repository.quotation;

import com.howellyoung.exchange.entity.base.BaseBarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseBarEntityRepository<T extends BaseBarEntity> extends JpaRepository<T, Long> {

    @Modifying
    @Query(value = "insert ignore into #{#entityName} (startTime, openPrice, highPrice, lowPrice, closePrice, quantity) values (:#{#entity.startTime}, :#{#entity.openPrice}, :#{#entity.highPrice}, :#{#entity.lowPrice}, :#{#entity.closePrice}, :#{#entity.quantity})", nativeQuery = true)
    void saveIgnore(T entity);
}