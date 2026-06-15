package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Cart;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String SESSION = "test-session";

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkoutEmptyCartThrows() {
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(SESSION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cart is empty");
    }

    @Test
    void checkoutCreatesOrderAndClearsCart() {
        Cart line = cartLine(1L, 2);
        Item item = item(1L, 500L);
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of(line));
        doAnswer(inv -> { ((Order) inv.getArgument(0)).setId(42L); return null; })
                .when(orderRepository).save(any(Order.class));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Long orderId = orderService.checkout(SESSION);

        assertThat(orderId).isEqualTo(42L);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(cartRepository).deleteAll(List.of(line));
    }

    @Test
    void getOrdersBuildsOrderList() {
        Order order = order(10L);
        OrderItem oi = orderItem(1L, 2, 100L);
        when(orderRepository.findBySessionIdOrderByOrderDateDesc(SESSION)).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(oi));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 999L)));

        List<OrderDto> orders = orderService.getOrders(SESSION);

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().getId()).isEqualTo(10L);
        assertThat(orders.getFirst().getTotalSum()).isEqualTo(200L);
    }

    @Test
    void getOrderByIdFound() {
        Order order = order(10L);
        OrderItem oi = orderItem(1L, 1, 100L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(oi));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 999L)));

        OrderDto result = orderService.getOrderById(SESSION, 10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTotalSum()).isEqualTo(100L);
    }

    @Test
    void getOrderByIdUsesHistoricalPrice() {
        Order order = order(10L);
        OrderItem oi = orderItem(1L, 1, 1500L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(oi));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 2000L)));

        OrderDto result = orderService.getOrderById(SESSION, 10L);

        assertThat(result.getTotalSum()).isEqualTo(1500L);
        assertThat(result.getItems().getFirst().getPrice()).isEqualTo(1500L);
    }

    @Test
    void getOrderByIdNotFound() {
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());

        assertThat(orderService.getOrderById(SESSION, 10L)).isNull();
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

    private Order order(long id) {
        Order order = new Order();
        order.setId(id);
        order.setSessionId(SESSION);
        return order;
    }

    private OrderItem orderItem(long itemId, int quantity, long priceAtPurchase) {
        OrderItem oi = new OrderItem();
        oi.setItemId(itemId);
        oi.setQuantity(quantity);
        oi.setPriceAtPurchase(priceAtPurchase);
        return oi;
    }
}
