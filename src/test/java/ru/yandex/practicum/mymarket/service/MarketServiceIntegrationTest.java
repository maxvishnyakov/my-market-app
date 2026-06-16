package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ItemView;
import ru.yandex.practicum.mymarket.dto.OrderDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketServiceIntegrationTest {

    private static final String SESSION = "integration-session";

    @Autowired
    private ItemService itemService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Test
    void getItemsSearchFindsItem() {
        Page<ItemView> page = itemService.getItems("Скакалка", "NO", 1, 10, SESSION);

        assertThat(page.getContent()).anyMatch(i -> i.getTitle().contains("Скакалка"));
    }

    @Test
    void getItemsPriceSortSortsByPrice() {
        Page<ItemView> page = itemService.getItems(null, "PRICE", 1, 10, SESSION);

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent().get(0).getPrice())
                .isLessThanOrEqualTo(page.getContent().get(1).getPrice());
    }

    @Test
    void getItemByIdReturnsItemFromDb() {
        Long id = itemService.getItems(null, "NO", 1, 1, SESSION).getContent().getFirst().getId();

        assertThat(itemService.getItemById(id)).isNotNull();
        assertThat(itemService.getItemById(id).getId()).isEqualTo(id);
    }

    @Test
    void updateCartMinusAndDelete() {
        ItemView item = itemService.getItems(null, "NO", 1, 1, SESSION).getContent().getFirst();

        cartService.updateCart(SESSION, item.getId(), "PLUS");
        cartService.updateCart(SESSION, item.getId(), "PLUS");
        assertThat(cartService.getItemCountInCart(SESSION, item.getId())).isEqualTo(2);

        cartService.updateCart(SESSION, item.getId(), "MINUS");
        assertThat(cartService.getItemCountInCart(SESSION, item.getId())).isEqualTo(1);

        cartService.updateCart(SESSION, item.getId(), "DELETE");
        assertThat(cartService.getCart(SESSION)).isEmpty();
    }

    @Test
    void checkoutEmptyCartThrows() {
        assertThatThrownBy(() -> orderService.checkout("empty-session"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addToCartCheckoutAndGetOrders() {
        ItemView item = itemService.getItems(null, "NO", 1, 1, SESSION).getContent().getFirst();

        cartService.updateCart(SESSION, item.getId(), "PLUS");
        cartService.updateCart(SESSION, item.getId(), "PLUS");

        assertThat(cartService.getCartTotal(SESSION)).isEqualTo(item.getPrice() * 2);

        Long orderId = orderService.checkout(SESSION);

        assertThat(cartService.getCart(SESSION)).isEmpty();

        OrderDto order = orderService.getOrderById(SESSION, orderId);
        assertThat(order).isNotNull();
        assertThat(order.getTotalSum()).isEqualTo(item.getPrice() * 2);

        List<OrderDto> orders = orderService.getOrders(SESSION);
        assertThat(orders).extracting(OrderDto::getId).contains(orderId);
    }

    @Test
    void getOrderByIdUnknownReturnsNull() {
        assertThat(orderService.getOrderById(SESSION, 999_999L)).isNull();
    }
}
