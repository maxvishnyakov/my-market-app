package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.mymarket.entity.Cart;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("SELECT c FROM Cart c WHERE c.sessionId = :sessionId AND c.isOrdered = false")
    List<Cart> findActiveCartBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT c FROM Cart c WHERE c.sessionId = :sessionId AND c.itemId = :itemId AND c.isOrdered = false")
    Optional<Cart> findActiveCartItem(@Param("sessionId") String sessionId,
                                      @Param("itemId") Long productId);

    @Query("SELECT DISTINCT c.orderId FROM Cart c WHERE c.sessionId = :sessionId AND c.isOrdered = true " +
            "AND c.orderId IS NOT NULL ORDER BY c.orderDate DESC")
    List<Long> findOrderIdsBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT c FROM Cart c WHERE c.sessionId = :sessionId AND c.isOrdered = true AND c.orderId = :orderId")
    List<Cart> findOrderByOrderId(@Param("sessionId") String sessionId,
                                  @Param("orderId") Long orderId);
}
