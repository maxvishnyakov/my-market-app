package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

@Controller
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public String getOrders(HttpSession session, Model model) {
        List<OrderDto> orders = orderService.getOrders(session.getId());
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable Long id,
                           @RequestParam(defaultValue = "false") boolean newOrder,
                           HttpSession session,
                           Model model) {
        OrderDto order = orderService.getOrderById(session.getId(), id);
        if (order == null) {
            return "redirect:/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);
        return "order";
    }

    @PostMapping("/buy")
    public String checkout(HttpSession session) {
        try {
            Long orderId = orderService.checkout(session.getId());
            return "redirect:/orders/" + orderId + "?newOrder=true";
        } catch (IllegalStateException e) {
            return "redirect:/cart/items";
        }
    }
}
