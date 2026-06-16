package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.entity.Item;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findAllReturnsTestData() {
        assertThat(itemRepository.findAll()).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void searchItemsFindsByTitle() {
        Page<Item> page = itemRepository.searchItems("Скакалка", PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Item::getTitle)
                .anyMatch(title -> title.contains("Скакалка"));
    }

    @Test
    void searchItemsFindsByDescription() {
        Page<Item> page = itemRepository.searchItems("йоги", PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Item::getTitle)
                .anyMatch(title -> title.toLowerCase().contains("йоги"));
    }

    @Test
    void findAllByOrderByTitleAscSortsAlphabetically() {
        Page<Item> page = itemRepository.findAllByOrderByTitleAsc(PageRequest.of(0, 10));

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
        String first = page.getContent().get(0).getTitle();
        String second = page.getContent().get(1).getTitle();
        assertThat(first.compareToIgnoreCase(second)).isLessThanOrEqualTo(0);
    }

    @Test
    void findAllByOrderByPriceAscSortsByPrice() {
        Page<Item> page = itemRepository.findAllByOrderByPriceAsc(PageRequest.of(0, 10));

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent().get(0).getPrice())
                .isLessThanOrEqualTo(page.getContent().get(1).getPrice());
    }
}
