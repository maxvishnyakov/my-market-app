package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.ItemView;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Cart;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Transactional
    public Long checkout(String sessionId) {
        List<Cart> cartItems = cartRepository.findBySessionId(sessionId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        Order order = new Order();
        order.setSessionId(sessionId);
        order.setOrderDate(LocalDateTime.now());
        orderRepository.save(order);
        Long orderId = order.getId();
        for (Cart cartItem : cartItems) {
            Item item = itemRepository.findById(cartItem.getItemId()).orElseThrow();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setItemId(cartItem.getItemId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(item.getPrice());
            orderItemRepository.save(orderItem);
        }
        cartRepository.deleteAll(cartItems);
        return orderId;
    }

    public List<OrderDto> getOrders(String sessionId) {
        return orderRepository.findBySessionIdOrderByOrderDateDesc(sessionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderById(String sessionId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getSessionId().equals(sessionId))
                .map(this::toDto)
                .orElse(null);
    }

    private OrderDto toDto(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        List<ItemView> items = new ArrayList<>();
        long total = 0;
        for (OrderItem oi : orderItems) {
            total += oi.getPriceAtPurchase() * oi.getQuantity();
            Item source = itemRepository.findById(oi.getItemId()).orElse(null);
            if (source != null) {
                items.add(new ItemView(source.getId(), source.getTitle(), source.getDescription(),
                        source.getImgPath(), oi.getPriceAtPurchase(), oi.getQuantity()));
            }
        }
        return new OrderDto(order.getId(), items, total);
    }
}
