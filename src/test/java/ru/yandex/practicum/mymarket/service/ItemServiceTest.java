package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.mymarket.dto.ItemView;
import ru.yandex.practicum.mymarket.entity.Cart;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private static final String SESSION = "test-session";

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getItemsSearchUsesSearchRepository() {
        Item item = item(1L, 100L);
        when(itemRepository.searchItems(eq("мяч"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of());

        itemService.getItems("  мяч ", "PRICE", 1, 5, SESSION);

        verify(itemRepository).searchItems("мяч", PageRequest.of(0, 5));
        verify(itemRepository, never()).findAllByOrderByPriceAsc(any());
    }

    @Test
    void getItemsAlphaSortUsesAlphaRepository() {
        when(itemRepository.findAllByOrderByTitleAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of());

        itemService.getItems(null, "ALPHA", 1, 5, SESSION);

        verify(itemRepository).findAllByOrderByTitleAsc(PageRequest.of(0, 5));
    }

    @Test
    void getItemsPriceSortUsesPriceRepository() {
        when(itemRepository.findAllByOrderByPriceAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of());

        itemService.getItems(null, "PRICE", 1, 5, SESSION);

        verify(itemRepository).findAllByOrderByPriceAsc(PageRequest.of(0, 5));
    }

    @Test
    void getItemsSetsCountFromCart() {
        Item item = item(1L, 100L);
        Cart line = cartLine(1L, 3);

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(item)));
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of(line));

        ItemView view = itemService.getItems(null, "NO", 1, 5, SESSION).getContent().getFirst();
        assertThat(view.getCount()).isEqualTo(3);
        assertThat(view.getPrice()).isEqualTo(100L);
    }

    @Test
    void getItemsEmptySearchFallsBackToDefault() {
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of());

        itemService.getItems("", "NO", 1, 5, SESSION);

        verify(itemRepository).findAll(any(Pageable.class));
        verify(itemRepository, never()).searchItems(any(), any());
    }

    @Test
    void getItemsBlankSearchFallsBackToDefault() {
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(cartRepository.findBySessionId(SESSION)).thenReturn(List.of());

        itemService.getItems("   ", "NO", 1, 5, SESSION);

        verify(itemRepository).findAll(any(Pageable.class));
        verify(itemRepository, never()).searchItems(any(), any());
    }

    @Test
    void getItemByIdFound() {
        Item item = item(1L, 100L);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThat(itemService.getItemById(1L)).isEqualTo(item);
    }

    @Test
    void getItemByIdNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(itemService.getItemById(1L)).isNull();
    }

    private Item item(long id, long price) {
        Item item = new Item();
        item.setId(id);
        item.setPrice(price);
        return item;
    }

    private Cart cartLine(long itemId, int quantity) {
        Cart cart = new Cart();
        cart.setItemId(itemId);
        cart.setQuantity(quantity);
        return cart;
    }
}
