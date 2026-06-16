package ru.yandex.practicum.mymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemView {
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private Long price;
    private int count;
}
