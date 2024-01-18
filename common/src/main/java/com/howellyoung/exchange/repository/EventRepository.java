package com.howellyoung.exchange.repository;

import com.howellyoung.exchange.entity.trade.EventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
    List<EventEntity> findBySequenceIdGreaterThanOrderByIdAsc(Long sequenceId, Pageable pageable);
}