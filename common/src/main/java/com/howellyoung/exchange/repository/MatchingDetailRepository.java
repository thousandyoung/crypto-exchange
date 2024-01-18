package com.howellyoung.exchange.repository;

import com.howellyoung.exchange.entity.trade.MatchingDetailEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MatchingDetailRepository extends JpaRepository<MatchingDetailEntity, Long> {
    @Transactional
    default void insertIgnore(MatchingDetailEntity entity, EntityManager em) {
        String tableName = em.getMetamodel().entity(MatchingDetailEntity.class).getName();
        Query query = em.createNativeQuery("insert ignore into " + tableName + " (id, ...) values (:id, ...)");
        // set parameters and execute query
    }
}