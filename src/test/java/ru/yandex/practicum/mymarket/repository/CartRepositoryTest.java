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
    void findBySessionIdReturnsAllCartLines() {
        Long itemId = itemRepository.findAll().getFirst().getId();

        Cart line1 = cartLine(SESSION, itemId, 1);
        Cart line2 = cartLine("other-session", itemId, 2);
        cartRepository.save(line1);
        cartRepository.save(line2);

        List<Cart> result = cartRepository.findBySessionId(SESSION);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getQuantity()).isEqualTo(1);
    }

    @Test
    void findBySessionIdAndItemIdReturnsLine() {
        Long itemId = itemRepository.findAll().getFirst().getId();

        cartRepository.save(cartLine(SESSION, itemId, 3));

        assertThat(cartRepository.findBySessionIdAndItemId(SESSION, itemId))
                .isPresent()
                .get()
                .extracting(Cart::getQuantity)
                .isEqualTo(3);
    }

    @Test
    void findBySessionIdAndItemIdEmptyForWrongSession() {
        Long itemId = itemRepository.findAll().getFirst().getId();

        cartRepository.save(cartLine("other-session", itemId, 1));

        assertThat(cartRepository.findBySessionIdAndItemId(SESSION, itemId)).isEmpty();
    }

    private Cart cartLine(String sessionId, Long itemId, int quantity) {
        Cart cart = new Cart();
        cart.setSessionId(sessionId);
        cart.setItemId(itemId);
        cart.setQuantity(quantity);
        return cart;
    }
}
