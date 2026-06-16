package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void getRootReturnsItemsPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }

    @Test
    void getItemsReturnsItemsPage() throws Exception {
        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));
    }

    @Test
    void postItemsUpdatesCartAndRedirects() throws Exception {
        Long itemId = firstItemId();

        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .param("sort", "PRICE")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=&sort=PRICE&pageNumber=1&pageSize=5"));
    }

    @Test
    void getItemReturnsItemPage() throws Exception {
        Long itemId = firstItemId();

        mockMvc.perform(get("/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(view().name("item"));
    }

    @Test
    void getItemNotFoundRedirectsToItems() throws Exception {
        mockMvc.perform(get("/items/{id}", 999_999L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }

    @Test
    void postItemUpdatesCartAndReturnsItemPage() throws Exception {
        Long itemId = firstItemId();

        mockMvc.perform(post("/items/{id}", itemId).param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"));
    }

    @Test
    void getCartReturnsCartPage() throws Exception {
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
    }

    @Test
    void postCartUpdatesCartAndReturnsCartPage() throws Exception {
        Long itemId = firstItemId();
        MockHttpSession session = addItemToCart(itemId);

        mockMvc.perform(post("/cart/items")
                        .session(session)
                        .param("id", itemId.toString())
                        .param("action", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
    }

    @Test
    void postBuyEmptyCartRedirectsToCart() throws Exception {
        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));
    }

    @Test
    void postBuyWithItemsRedirectsToOrder() throws Exception {
        MockHttpSession session = addItemToCart(firstItemId());

        mockMvc.perform(post("/buy").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*?newOrder=true"));
    }

    @Test
    void getOrdersReturnsOrdersPage() throws Exception {
        MockHttpSession session = buyItemInSession();

        mockMvc.perform(get("/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"));
    }

    @Test
    void getOrderReturnsOrderPage() throws Exception {
        MockHttpSession session = addItemToCart(firstItemId());
        Long orderId = checkoutAndGetOrderId(session);

        mockMvc.perform(get("/orders/{id}", orderId).param("newOrder", "true").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("order"));
    }

    @Test
    void getOrderNotFoundRedirectsToOrders() throws Exception {
        mockMvc.perform(get("/orders/{id}", 999_999L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
    }

    private Long firstItemId() {
        return itemRepository.findAll().getFirst().getId();
    }

    private MockHttpSession addItemToCart(Long itemId) throws Exception {
        MvcResult result = mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    private MockHttpSession buyItemInSession() throws Exception {
        MockHttpSession session = addItemToCart(firstItemId());
        checkoutAndGetOrderId(session);
        return session;
    }

    private Long checkoutAndGetOrderId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/buy").session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String url = result.getResponse().getRedirectedUrl();
        String idPart = url.substring(url.indexOf("/orders/") + "/orders/".length());
        if (idPart.contains("?")) {
            idPart = idPart.substring(0, idPart.indexOf('?'));
        }
        return Long.parseLong(idPart);
    }
}
