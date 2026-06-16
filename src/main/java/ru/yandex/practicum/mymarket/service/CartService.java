package ru.yandex.practicum.mymarket.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.ItemView;
import ru.yandex.practicum.mymarket.entity.Cart;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    public int getItemCountInCart(String sessionId, Long itemId) {
        return cartRepository.findBySessionIdAndItemId(sessionId, itemId)
                .map(Cart::getQuantity)
                .orElse(0);
    }

    @Transactional
    public void updateCart(String sessionId, Long itemId, String action) {
        Optional<Cart> existing = cartRepository.findBySessionIdAndItemId(sessionId, itemId);
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

    public List<ItemView> getCart(String sessionId) {
        List<Cart> cartItems = cartRepository.findBySessionId(sessionId);
        List<ItemView> products = new ArrayList<>();
        for (Cart cartItem : cartItems) {
            Item item = itemRepository.findById(cartItem.getItemId()).orElse(null);
            if (item != null) {
                products.add(new ItemView(item.getId(), item.getTitle(), item.getDescription(),
                        item.getImgPath(), item.getPrice(), cartItem.getQuantity()));
            }
        }
        return products;
    }

    public long getCartTotal(String sessionId) {
        return getCart(sessionId).stream()
                .mapToLong(v -> v.getPrice() * v.getCount())
                .sum();
    }
}
