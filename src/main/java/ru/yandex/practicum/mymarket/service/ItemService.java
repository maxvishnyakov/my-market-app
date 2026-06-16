package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.ItemView;
import ru.yandex.practicum.mymarket.entity.Cart;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    public Page<ItemView> getItems(String search, String sort, int pageNumber, int pageSize, String sessionId) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        Page<Item> itemsPage;

        if (search != null && !search.trim().isEmpty()) {
            itemsPage = itemRepository.searchItems(search.trim(), pageable);
        } else {
            itemsPage = switch (sort) {
                case "ALPHA" -> itemRepository.findAllByOrderByTitleAsc(pageable);
                case "PRICE" -> itemRepository.findAllByOrderByPriceAsc(pageable);
                default -> itemRepository.findAll(pageable);
            };
        }
        List<Cart> cartItems = cartRepository.findBySessionId(sessionId);
        Map<Long, Integer> cartMap = cartItems.stream()
                .collect(Collectors.toMap(Cart::getItemId, Cart::getQuantity));
        return itemsPage.map(item -> new ItemView(
                item.getId(), item.getTitle(), item.getDescription(), item.getImgPath(),
                item.getPrice(), cartMap.getOrDefault(item.getId(), 0)
        ));
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }
}
