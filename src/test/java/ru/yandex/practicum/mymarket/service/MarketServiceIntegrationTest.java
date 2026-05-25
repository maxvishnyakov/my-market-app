package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketServiceIntegrationTest {

    private static final String SESSION = "integration-session";

    @Autowired
    private MarketService marketService;

    @Test
    void getItemsSearchFindsItem() {
        Page<Item> page = marketService.getItems("Скакалка", "NO", 1, 10, SESSION);

        assertThat(page.getContent()).anyMatch(i -> i.getTitle().contains("Скакалка"));
    }

    @Test
    void getItemsPriceSortSortsByPrice() {
        Page<Item> page = marketService.getItems(null, "PRICE", 1, 10, SESSION);

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent().get(0).getPrice())
                .isLessThanOrEqualTo(page.getContent().get(1).getPrice());
    }

    @Test
    void getItemByIdReturnsItemFromDb() {
        Long id = marketService.getItems(null, "NO", 1, 1, SESSION).getContent().getFirst().getId();

        Item item = marketService.getItemById(id);

        assertThat(item).isNotNull();
        assertThat(item.getId()).isEqualTo(id);
    }

    @Test
    void updateCartMinusAndDelete() {
        Item item = marketService.getItems(null, "NO", 1, 1, SESSION).getContent().getFirst();

        marketService.updateCart(SESSION, item.getId(), "PLUS");
        marketService.updateCart(SESSION, item.getId(), "PLUS");
        assertThat(marketService.getItemCountInCart(SESSION, item.getId())).isEqualTo(2);

        marketService.updateCart(SESSION, item.getId(), "MINUS");
        assertThat(marketService.getItemCountInCart(SESSION, item.getId())).isEqualTo(1);

        marketService.updateCart(SESSION, item.getId(), "DELETE");
        assertThat(marketService.getCart(SESSION)).isEmpty();
    }

    @Test
    void checkoutEmptyCartThrows() {
        assertThatThrownBy(() -> marketService.checkout("empty-session"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addToCartCheckoutAndGetOrders() {
        Item item = marketService.getItems(null, "NO", 1, 1, SESSION).getContent().getFirst();

        marketService.updateCart(SESSION, item.getId(), "PLUS");
        marketService.updateCart(SESSION, item.getId(), "PLUS");

        assertThat(marketService.getCartTotal(SESSION)).isEqualTo(item.getPrice() * 2);

        Long orderId = marketService.checkout(SESSION);

        assertThat(marketService.getCart(SESSION)).isEmpty();

        OrderDto order = marketService.getOrderById(SESSION, orderId);
        assertThat(order).isNotNull();
        assertThat(order.getTotalSum()).isEqualTo(item.getPrice() * 2);

        List<OrderDto> orders = marketService.getOrders(SESSION);
        assertThat(orders).extracting(OrderDto::getId).contains(orderId);
    }

    @Test
    void getOrderByIdUnknownReturnsNull() {
        assertThat(marketService.getOrderById(SESSION, 999_999L)).isNull();
    }
}
