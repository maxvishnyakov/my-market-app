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
import ru.yandex.practicum.mymarket.dto.ItemView;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ItemController {
    @Autowired
    private ItemService itemService;

    @Autowired
    private CartService cartService;

    @GetMapping({"/", "/items"})
    public String getItems(@RequestParam(required = false) String search,
                           @RequestParam(defaultValue = "NO") String sort,
                           @RequestParam(defaultValue = "1") int pageNumber,
                           @RequestParam(defaultValue = "5") int pageSize,
                           HttpSession session,
                           Model model) {
        Page<ItemView> productsPage = itemService.getItems(search, sort, pageNumber, pageSize, session.getId());
        List<List<ItemView>> itemsGrid = new ArrayList<>();
        List<ItemView> itemsList = productsPage.getContent();
        for (int i = 0; i < itemsList.size(); i += 3) {
            List<ItemView> row = new ArrayList<>();
            for (int j = i; j < i + 3 && j < itemsList.size(); j++) {
                row.add(itemsList.get(j));
            }
            while (row.size() < 3) {
                row.add(new ItemView(-1L, null, null, null, 0L, 0));
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
        cartService.updateCart(session.getId(), id, action);
        return String.format("redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d",
                search != null ? search : "", sort, pageNumber, pageSize);
    }

    @GetMapping("/items/{id}")
    public String getItem(@PathVariable Long id, HttpSession session, Model model) {
        Item item = itemService.getItemById(id);
        if (item == null || item.getId() == null) {
            return "redirect:/items";
        }
        int count = cartService.getItemCountInCart(session.getId(), id);
        model.addAttribute("item", new ItemView(item.getId(), item.getTitle(), item.getDescription(),
                item.getImgPath(), item.getPrice(), count));
        return "item";
    }

    @PostMapping("/items/{id}")
    public String updateCartFromItem(@PathVariable Long id,
                                     @RequestParam String action,
                                     HttpSession session,
                                     Model model) {
        cartService.updateCart(session.getId(), id, action);
        Item item = itemService.getItemById(id);
        int count = cartService.getItemCountInCart(session.getId(), id);
        model.addAttribute("item", new ItemView(item.getId(), item.getTitle(), item.getDescription(),
                item.getImgPath(), item.getPrice(), count));
        return "item";
    }
}
