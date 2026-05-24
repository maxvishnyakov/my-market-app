package ru.yandex.practicum.mymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private List<Item> items;
    private long totalSum;
}
