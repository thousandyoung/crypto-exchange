package com.howellyoung.exchange.repository;


import com.howellyoung.exchange.entity.OrderEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @Transactional
    default void insertIgnore(OrderEntity entity, EntityManager em) {
        String tableName = em.getMetamodel().entity(OrderEntity.class).getName();
        Query query = em.createNativeQuery("insert ignore into " + tableName + " (id, ...) values (:id, ...)");
        // set parameters and execute query
    }
}