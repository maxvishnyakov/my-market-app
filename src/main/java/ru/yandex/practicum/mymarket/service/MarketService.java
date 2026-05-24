package ru.yandex.practicum.mymarket.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Cart;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MarketService {
    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    public Page<Item> getItems(String search, String sort, int pageNumber, int pageSize, String sessionId) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        Page<Item> itemsPage;

        if (search != null && !search.trim().isEmpty()) {
            itemsPage = itemRepository.searchItems(search.trim(), pageable);
        } else {
            switch (sort) {
                case "ALPHA":
                    itemsPage = itemRepository.findAllByOrderByTitleAsc(pageable);
                    break;
                case "PRICE":
                    itemsPage = itemRepository.findAllByOrderByPriceAsc(pageable);
                    break;
                default:
                    itemsPage = itemRepository.findAll(pageable);
            }
        }
        List<Cart> cartItems = cartRepository.findActiveCartBySessionId(sessionId);
        Map<Long, Integer> cartMap = cartItems.stream()
                .collect(Collectors.toMap(Cart::getItemId, Cart::getQuantity));
        itemsPage.getContent().forEach(product ->
                product.setCount(cartMap.getOrDefault(product.getId(), 0))
        );
        return itemsPage;
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    public int getItemCountInCart(String sessionId, Long itemId) {
        return cartRepository.findActiveCartItem(sessionId, itemId)
                .map(Cart::getQuantity)
                .orElse(0);
    }

    @Transactional
    public void updateCart(String sessionId, Long itemId, String action) {
        Optional<Cart> existing = cartRepository.findActiveCartItem(sessionId, itemId);
        switch (action.toUpperCase()) {
            case "PLUS":
                if (existing.isPresent()) {
                    Cart cart = existing.get();
                    cart.setQuantity(cart.getQuantity() + 1);
                    cartRepository.save(cart);
                } else {
                    Cart newItem = new Cart();
                    newItem.setSessionId(sessionId);
                    newItem.setItemId(itemId);
                    newItem.setQuantity(1);
                    newItem.setIsOrdered(false);
                    cartRepository.save(newItem);
                }
                break;
            case "MINUS":
                if (existing.isPresent()) {
                    Cart cart = existing.get();
                    if (cart.getQuantity() > 1) {
                        cart.setQuantity(cart.getQuantity() - 1);
                        cartRepository.save(cart);
                    } else {
                        cartRepository.delete(cart);
                    }
                }
                break;
            case "DELETE":
                existing.ifPresent(cartRepository::delete);
                break;
        }
    }

    public List<Item> getCart(String sessionId) {
        List<Cart> cartItems = cartRepository.findActiveCartBySessionId(sessionId);
        List<Item> products = new ArrayList<>();
        for (Cart item : cartItems) {
            Item product = itemRepository.findById(item.getItemId()).orElse(null);
            if (product != null) {
                product.setCount(item.getQuantity());
                products.add(product);
            }
        }
        return products;
    }

    public long getCartTotal(String sessionId) {
        return getCart(sessionId).stream()
                .mapToLong(p -> p.getPrice() * p.getCount())
                .sum();
    }

    @Transactional
    public Long checkout(String sessionId) {
        List<Cart> cartItems = cartRepository.findActiveCartBySessionId(sessionId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        Long orderId = System.currentTimeMillis();
        LocalDateTime orderDate = LocalDateTime.now();
        for (Cart item : cartItems) {
            item.setIsOrdered(true);
            item.setOrderId(orderId);
            item.setOrderDate(orderDate);
        }
        cartRepository.saveAll(cartItems);
        return orderId;
    }

    public List<OrderDto> getOrders(String sessionId) {
        List<Long> orderIds = cartRepository.findOrderIdsBySessionId(sessionId);
        List<OrderDto> orders = new ArrayList<>();
        for (Long orderId : orderIds) {
            List<Cart> orderItems = cartRepository.findOrderByOrderId(sessionId, orderId);
            List<Item> items = new ArrayList<>();
            long totalSum = 0;
            for (Cart cart : orderItems) {
                Item item = itemRepository.findById(cart.getItemId()).orElse(null);
                if (item != null) {
                    item.setCount(cart.getQuantity());
                    items.add(item);
                    totalSum += item.getPrice() * cart.getQuantity();
                }
            }
            orders.add(new OrderDto(orderId, items, totalSum));
        }
        return orders;
    }

    public OrderDto getOrderById(String sessionId, Long orderId) {
        List<Cart> orderItems = cartRepository.findOrderByOrderId(sessionId, orderId);
        if (orderItems.isEmpty()) {
            return null;
        }
        List<Item> products = new ArrayList<>();
        long totalSum = 0;
        for (Cart item : orderItems) {
            Item product = itemRepository.findById(item.getItemId()).orElse(null);
            if (product != null) {
                product.setCount(item.getQuantity());
                products.add(product);
                totalSum += product.getPrice() * item.getQuantity();
            }
        }
        return new OrderDto(orderId, products, totalSum);
    }
}
