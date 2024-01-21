package com.howellyoung.exchange.repository.trade;


import com.howellyoung.exchange.entity.trade.OrderEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @Transactional
    default void insertIgnore(OrderEntity entity, EntityManager em) {
        String tableName = em.getMetamodel().entity(OrderEntity.class).getName();
        Query query = em.createNativeQuery("insert ignore into " + tableName +
                " (id, sequenceId, direction, userId, status, price, createdAt, updatedAt, quantity, unfilledQuantity) " +
                "values (:id, :sequenceId, :direction, :userId, :status, :price, :createdAt, :updatedAt, :quantity, :unfilledQuantity)");
        query.setParameter("id", entity.id);
        query.setParameter("sequenceId", entity.sequenceId);
        query.setParameter("direction", entity.direction.toString());
        query.setParameter("userId", entity.userId);
        query.setParameter("status", entity.status.toString());
        query.setParameter("price", entity.price);
        query.setParameter("createdAt", entity.createdAt);
        query.setParameter("updatedAt", entity.updatedAt);
        query.setParameter("quantity", entity.quantity);
        query.setParameter("unfilledQuantity", entity.unfilledQuantity);
        query.executeUpdate();
    }
    List<OrderEntity> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Optional<Object> findByIdAndUserId(Long orderId, Long userId);
}