package com.howellyoung.exchange.repository.trade;
import com.howellyoung.exchange.entity.trade.UniqueEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniqueEventRepository extends JpaRepository<UniqueEventEntity, String> {
}