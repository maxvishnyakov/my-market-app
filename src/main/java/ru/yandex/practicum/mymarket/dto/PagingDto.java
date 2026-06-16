package ru.yandex.practicum.mymarket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagingDto {
    private int pageSize;
    private int pageNumber;
    private boolean hasPrevious;
    private boolean hasNext;
}
