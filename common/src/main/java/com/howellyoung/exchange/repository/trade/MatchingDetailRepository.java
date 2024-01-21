package com.howellyoung.exchange.repository.trade;

import com.howellyoung.exchange.entity.trade.MatchingDetailEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchingDetailRepository extends JpaRepository<MatchingDetailEntity, Long> {
    @Transactional
    default void insertIgnore(MatchingDetailEntity entity, EntityManager em) {
        String tableName = em.getMetamodel().entity(MatchingDetailEntity.class).getName();
        Query query = em.createNativeQuery("insert ignore into " + tableName +
                " (id, sequenceId, orderId, counterOrderId, userId, counterUserId, type, direction, price, quantity, createdAt) " +
                "values (:id, :sequenceId, :orderId, :counterOrderId, :userId, :counterUserId, :type, :direction, :price, :quantity, :createdAt)");
        query.setParameter("id", entity.id);
        query.setParameter("sequenceId", entity.sequenceId);
        query.setParameter("orderId", entity.orderId);
        query.setParameter("counterOrderId", entity.counterOrderId);
        query.setParameter("userId", entity.userId);
        query.setParameter("counterUserId", entity.counterUserId);
        query.setParameter("type", entity.type.toString());
        query.setParameter("direction", entity.direction.toString());
        query.setParameter("price", entity.price);
        query.setParameter("quantity", entity.quantity);
        query.setParameter("createdAt", entity.createdAt);
        query.executeUpdate();
    }

    List<MatchingDetailEntity> findByOrderIdOrderById(Long orderId);
}