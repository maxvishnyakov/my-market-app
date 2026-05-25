package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Cart;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    private static final String SESSION = "test-session";

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private MarketService marketService;

    @Test
    void getItemsSearchUsesSearchRepository() {
        Item item = item(1L, 100L);
        when(itemRepository.searchItems(eq("мяч"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));
        when(cartRepository.findActiveCartBySessionId(SESSION)).thenReturn(List.of());

        marketService.getItems("  мяч ", "PRICE", 1, 5, SESSION);

        verify(itemRepository).searchItems("мяч", PageRequest.of(0, 5));
        verify(itemRepository, never()).findAllByOrderByPriceAsc(any());
    }

    @Test
    void getItemsAlphaSortUsesAlphaRepository() {
        when(itemRepository.findAllByOrderByTitleAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(cartRepository.findActiveCartBySessionId(SESSION)).thenReturn(List.of());

        marketService.getItems(null, "ALPHA", 1, 5, SESSION);

        verify(itemRepository).findAllByOrderByTitleAsc(PageRequest.of(0, 5));
    }

    @Test
    void getItemsPriceSortUsesPriceRepository() {
        when(itemRepository.findAllByOrderByPriceAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(cartRepository.findActiveCartBySessionId(SESSION)).thenReturn(List.of());

        marketService.getItems(null, "PRICE", 1, 5, SESSION);

        verify(itemRepository).findAllByOrderByPriceAsc(PageRequest.of(0, 5));
    }

    @Test
    void getItemsSetsCountFromCart() {
        Item item = item(1L, 100L);
        Cart line = cartLine(1L, 3);

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(item)));
        when(cartRepository.findActiveCartBySessionId(SESSION)).thenReturn(List.of(line));

        assertThat(marketService.getItems(null, "NO", 1, 5, SESSION).getContent().getFirst().getCount())
                .isEqualTo(3);
    }

    @Test
    void getItemByIdFound() {
        Item item = item(1L, 100L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThat(marketService.getItemById(1L)).isEqualTo(item);
    }

    @Test
    void getItemByIdNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(marketService.getItemById(1L)).isNull();
    }

    @Test
    void getItemCountInCartEmpty() {
        when(cartRepository.findActiveCartItem(SESSION, 1L)).thenReturn(Optional.empty());

        assertThat(marketService.getItemCountInCart(SESSION, 1L)).isZero();
    }

    @Test
    void getItemCountInCartReturnsQuantity() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findActiveCartItem(SESSION, 1L)).thenReturn(Optional.of(line));

        assertThat(marketService.getItemCountInCart(SESSION, 1L)).isEqualTo(2);
    }

    @Test
    void updateCartPlusSavesNewLine() {
        when(cartRepository.findActiveCartItem(SESSION, 1L)).thenReturn(Optional.empty());

        marketService.updateCart(SESSION, 1L, "PLUS");

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void updateCartPlusIncrementsQuantity() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findActiveCartItem(SESSION, 1L)).thenReturn(Optional.of(line));

        marketService.updateCart(SESSION, 1L, "PLUS");

        assertThat(line.getQuantity()).isEqualTo(3);
        verify(cartRepository).save(line);
    }

    @Test
    void updateCartMinusDecrementsQuantity() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findActiveCartItem(SESSION, 1L)).thenReturn(Optional.of(line));

        marketService.updateCart(SESSION, 1L, "MINUS");

        assertThat(line.getQuantity()).isEqualTo(1);
        verify(cartRepository).save(line);
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void updateCartMinusDeletesLastItem() {
        Cart line = cartLine(1L, 1);
        when(cartRepository.findActiveCartItem(SESSION, 1L)).thenReturn(Optional.of(line));

        marketService.updateCart(SESSION, 1L, "MINUS");

        verify(cartRepository).delete(line);
    }

    @Test
    void updateCartDeleteRemovesLine() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findActiveCartItem(SESSION, 1L)).thenReturn(Optional.of(line));

        marketService.updateCart(SESSION, 1L, "DELETE");

        verify(cartRepository).delete(line);
    }

    @Test
    void getCartReturnsItemsWithCount() {
        Cart line = cartLine(1L, 2);
        Item item = item(1L, 100L);

        when(cartRepository.findActiveCartBySessionId(SESSION)).thenReturn(List.of(line));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        List<Item> cart = marketService.getCart(SESSION);

        assertThat(cart).hasSize(1);
        assertThat(cart.getFirst().getCount()).isEqualTo(2);
    }

    @Test
    void getCartTotalCalculatesSum() {
        Cart line = cartLine(1L, 2);
        when(cartRepository.findActiveCartBySessionId(SESSION)).thenReturn(List.of(line));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 100L)));

        assertThat(marketService.getCartTotal(SESSION)).isEqualTo(200L);
    }

    @Test
    void checkoutEmptyCartThrows() {
        when(cartRepository.findActiveCartBySessionId(SESSION)).thenReturn(List.of());

        assertThatThrownBy(() -> marketService.checkout(SESSION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cart is empty");
    }

    @Test
    void checkoutMarksLinesOrdered() {
        Cart line = cartLine(1L, 1);
        when(cartRepository.findActiveCartBySessionId(SESSION)).thenReturn(List.of(line));

        Long orderId = marketService.checkout(SESSION);

        assertThat(orderId).isNotNull();
        assertThat(line.getIsOrdered()).isTrue();
        assertThat(line.getOrderId()).isEqualTo(orderId);
        assertThat(line.getOrderDate()).isNotNull();
        verify(cartRepository).saveAll(List.of(line));
    }

    @Test
    void getOrdersBuildsOrderList() {
        Cart line = new Cart();
        line.setItemId(1L);
        line.setQuantity(2);

        when(cartRepository.findOrderIdsBySessionId(SESSION)).thenReturn(List.of(10L));
        when(cartRepository.findOrderByOrderId(SESSION, 10L)).thenReturn(List.of(line));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 100L)));

        List<OrderDto> orders = marketService.getOrders(SESSION);

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().getId()).isEqualTo(10L);
        assertThat(orders.getFirst().getTotalSum()).isEqualTo(200L);
    }

    @Test
    void getOrderByIdFound() {
        Cart line = cartLine(1L, 1);
        when(cartRepository.findOrderByOrderId(SESSION, 10L)).thenReturn(List.of(line));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 100L)));

        OrderDto order = marketService.getOrderById(SESSION, 10L);

        assertThat(order.getId()).isEqualTo(10L);
        assertThat(order.getTotalSum()).isEqualTo(100L);
    }

    @Test
    void getOrderByIdNotFound() {
        when(cartRepository.findOrderByOrderId(SESSION, 10L)).thenReturn(List.of());

        assertThat(marketService.getOrderById(SESSION, 10L)).isNull();
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
