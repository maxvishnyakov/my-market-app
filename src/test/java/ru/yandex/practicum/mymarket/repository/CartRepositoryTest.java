package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.entity.Cart;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartRepositoryTest {

    private static final String SESSION = "repo-session";

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findActiveCartBySessionIdReturnsOnlyActiveLines() {
        Long itemId = itemRepository.findAll().getFirst().getId();

        Cart active = new Cart();
        active.setSessionId(SESSION);
        active.setItemId(itemId);
        active.setQuantity(1);
        active.setIsOrdered(false);
        cartRepository.save(active);

        Cart ordered = new Cart();
        ordered.setSessionId(SESSION);
        ordered.setItemId(itemId);
        ordered.setQuantity(1);
        ordered.setIsOrdered(true);
        ordered.setOrderId(100L);
        cartRepository.save(ordered);

        List<Cart> activeLines = cartRepository.findActiveCartBySessionId(SESSION);

        assertThat(activeLines).hasSize(1);
        assertThat(activeLines.getFirst().getIsOrdered()).isFalse();
    }

    @Test
    void findActiveCartItemReturnsLineForSessionAndItem() {
        Long itemId = itemRepository.findAll().getFirst().getId();

        Cart line = new Cart();
        line.setSessionId(SESSION);
        line.setItemId(itemId);
        line.setQuantity(2);
        line.setIsOrdered(false);
        cartRepository.save(line);

        assertThat(cartRepository.findActiveCartItem(SESSION, itemId))
                .isPresent()
                .get()
                .extracting(Cart::getQuantity)
                .isEqualTo(2);
    }

    @Test
    void findActiveCartItemEmptyWhenOrdered() {
        Long itemId = itemRepository.findAll().getFirst().getId();

        Cart ordered = new Cart();
        ordered.setSessionId(SESSION);
        ordered.setItemId(itemId);
        ordered.setQuantity(1);
        ordered.setIsOrdered(true);
        ordered.setOrderId(200L);
        cartRepository.save(ordered);

        assertThat(cartRepository.findActiveCartItem(SESSION, itemId)).isEmpty();
    }

    @Test
    void findOrderIdsBySessionIdAndFindOrderByOrderId() {
        Long itemId = itemRepository.findAll().getFirst().getId();

        Cart line = new Cart();
        line.setSessionId(SESSION);
        line.setItemId(itemId);
        line.setQuantity(1);
        line.setIsOrdered(true);
        line.setOrderId(300L);
        line.setOrderDate(java.time.LocalDateTime.now());
        cartRepository.save(line);

        List<Long> orderIds = cartRepository.findOrderIdsBySessionId(SESSION);
        List<Cart> orderLines = cartRepository.findOrderByOrderId(SESSION, 300L);

        assertThat(orderIds).containsExactly(300L);
        assertThat(orderLines).hasSize(1);
        assertThat(orderLines.getFirst().getItemId()).isEqualTo(itemId);
    }
}
