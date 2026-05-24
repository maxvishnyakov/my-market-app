package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.service.MarketService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class MarketController {
    @Autowired
    private MarketService marketService;

    @GetMapping({"/", "/items"})
    public String getItems(@RequestParam(required = false) String search,
                           @RequestParam(defaultValue = "NO") String sort,
                           @RequestParam(defaultValue = "1") int pageNumber,
                           @RequestParam(defaultValue = "5") int pageSize,
                           HttpSession session,
                           Model model) {
        String sessionId = session.getId();
        Page<Item> productsPage = marketService.getItems(search, sort, pageNumber, pageSize, sessionId);
        List<List<Item>> itemsGrid = new ArrayList<>();
        List<Item> itemsList = productsPage.getContent();
        for (int i = 0; i < itemsList.size(); i += 3) {
            List<Item> row = new ArrayList<>();
            for (int j = i; j < i + 3 && j < itemsList.size(); j++) {
                row.add(itemsList.get(j));
            }
            while (row.size() < 3) {
                Item dummy = new Item();
                dummy.setId(-1L);
                row.add(dummy);
            }
            itemsGrid.add(row);
        }
        PagingDto paging = new PagingDto();
        paging.setPageSize(pageSize);
        paging.setPageNumber(pageNumber);
        paging.setHasPrevious(productsPage.hasPrevious());
        paging.setHasNext(productsPage.hasNext());
        model.addAttribute("items", itemsGrid);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("sort", sort);
        model.addAttribute("paging", paging);
        return "items";
    }

    @PostMapping("/items")
    public String updateCartFromItems(@RequestParam Long id,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(defaultValue = "NO") String sort,
                                      @RequestParam(defaultValue = "1") int pageNumber,
                                      @RequestParam(defaultValue = "5") int pageSize,
                                      @RequestParam String action,
                                      HttpSession session) {
        marketService.updateCart(session.getId(), id, action);
        return String.format("redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d",
                search != null ? search : "", sort, pageNumber, pageSize);
    }

    @GetMapping("/items/{id}")
    public String getItem(@PathVariable Long id, HttpSession session, Model model) {
        Item item = marketService.getItemById(id);
        if (item == null || item.getId() == null) {
            return "redirect:/items";
        }
        int count = marketService.getItemCountInCart(session.getId(), id);
        item.setCount(count);
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/items/{id}")
    public String updateCartFromItem(@PathVariable Long id,
                                        @RequestParam String action,
                                        HttpSession session,
                                        Model model) {
        marketService.updateCart(session.getId(), id, action);
        Item item = marketService.getItemById(id);
        item.setCount(marketService.getItemCountInCart(session.getId(), id));
        model.addAttribute("item", item);
        return "item";
    }

    @GetMapping("/cart/items")
    public String getCart(HttpSession session, Model model) {
        List<Item> cartItems = marketService.getCart(session.getId());
        long total = marketService.getCartTotal(session.getId());
        model.addAttribute("items", cartItems);
        model.addAttribute("total", total);
        return "cart";
    }

    @PostMapping("/cart/items")
    public String updateCartFromCartPage(@RequestParam Long id,
                                         @RequestParam String action,
                                         HttpSession session,
                                         Model model) {
        marketService.updateCart(session.getId(), id, action);
        List<Item> cartItems = marketService.getCart(session.getId());
        long total = marketService.getCartTotal(session.getId());
        model.addAttribute("items", cartItems);
        model.addAttribute("total", total);
        return "cart";
    }

    @PostMapping("/buy")
    public String checkout(HttpSession session) {
        try {
            Long orderId = marketService.checkout(session.getId());
            return "redirect:/orders/" + orderId + "?newOrder=true";
        } catch (IllegalStateException e) {
            return "redirect:/cart/items";
        }
    }

    @GetMapping("/orders")
    public String getOrders(HttpSession session, Model model) {
        List<OrderDto> orders = marketService.getOrders(session.getId());
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable Long id,
                           @RequestParam(defaultValue = "false") boolean newOrder,
                           HttpSession session,
                           Model model) {
        OrderDto order = marketService.getOrderById(session.getId(), id);

        if (order == null) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);
        return "order";
    }
}
