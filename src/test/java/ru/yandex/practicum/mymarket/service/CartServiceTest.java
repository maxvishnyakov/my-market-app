package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.dto.ItemView;
import ru.yandex.practicum.mymarket.entity.Cart;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String SESSION = "test-session";

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void getItemCountInCartEmpty() {
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.empty());

        assertThat(cartService.getItemCountInCart(SESSION, 1L)).isZero();
    }

    @Test
    void getItemCountInCartReturnsQuantity() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.of(line));

        assertThat(cartService.getItemCountInCart(SESSION, 1L)).isEqualTo(2);
    }

    @Test
    void updateCartPlusSavesNewLine() {
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.empty());

        cartService.updateCart(SESSION, 1L, "PLUS");

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void updateCartPlusIncrementsQuantity() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.of(line));

        cartService.updateCart(SESSION, 1L, "PLUS");

        assertThat(line.getQuantity()).isEqualTo(3);
        verify(cartRepository).save(line);
    }

    @Test
    void updateCartMinusDecrementsQuantity() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.of(line));

        cartService.updateCart(SESSION, 1L, "MINUS");

        assertThat(line.getQuantity()).isEqualTo(1);
        verify(cartRepository).save(line);
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void updateCartMinusDeletesLastItem() {
        Cart line = cartLine(1L, 1);
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.of(line));

        cartService.updateCart(SESSION, 1L, "MINUS");

        verify(cartRepository).delete(line);
    }

    @Test
    void updateCartDeleteRemovesLine() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.of(line));

        cartService.updateCart(SESSION, 1L, "DELETE");

        verify(cartRepository).delete(line);
    }

    @Test
    void updateCartMinusDoesNothingWhenItemAbsent() {
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.empty());

        cartService.updateCart(SESSION, 1L, "MINUS");

        verify(cartRepository, never()).save(any());
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void updateCartDeleteDoesNothingWhenItemAbsent() {
        when(cartRepository.findBySessionIdAndItemId(SESSION, 1L)).thenReturn(Optional.empty());

        cartService.updateCart(SESSION, 1L, "DELETE");

        verify(cartRepository, never()).delete(any());
    }

    @Test
    void getCartReturnsItemViewsWithCount() {
        Cart line = cartLine(1L, 2);
        Item item = item(1L, 100L);

        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of(line));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        List<ItemView> cart = cartService.getCart(SESSION);

        assertThat(cart).hasSize(1);
        assertThat(cart.getFirst().getCount()).isEqualTo(2);
        assertThat(cart.getFirst().getPrice()).isEqualTo(100L);
    }

    @Test
    void getCartTotalCalculatesSum() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of(line));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 100L)));

        assertThat(cartService.getCartTotal(SESSION)).isEqualTo(200L);
    }

    private Item item(long id, long price) {
        Item item = new Item();
        item.setId(id);
        item.setPrice(price);
        return item;
    }

    private Cart cartLine(long itemId, int quantity) {
        Cart cart = new Cart();
        cart.setItemId(itemId);
        cart.setQuantity(quantity);
        return cart;
    }
}
