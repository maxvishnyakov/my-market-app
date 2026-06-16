package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.ItemView;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    @GetMapping("/cart/items")
    public String getCart(HttpSession session, Model model) {
        List<ItemView> cartItems = cartService.getCart(session.getId());
        long total = cartService.getCartTotal(session.getId());
        model.addAttribute("items", cartItems);
        model.addAttribute("total", total);
        return "cart";
    }

    @PostMapping("/cart/items")
    public String updateCartFromCartPage(@RequestParam Long id,
                                         @RequestParam String action,
                                         HttpSession session,
                                         Model model) {
        cartService.updateCart(session.getId(), id, action);
        List<ItemView> cartItems = cartService.getCart(session.getId());
        long total = cartService.getCartTotal(session.getId());
        model.addAttribute("items", cartItems);
        model.addAttribute("total", total);
        return "cart";
    }
}
