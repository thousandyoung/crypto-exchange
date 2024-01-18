package com.howellyoung.exchange.repository;
import com.howellyoung.exchange.entity.UniqueEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniqueEventRepository extends JpaRepository<UniqueEventEntity, String> {
}