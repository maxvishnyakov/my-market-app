package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.mymarket.entity.Order;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findBySessionIdOrderByOrderDateDesc(String sessionId);
}
