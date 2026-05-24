package ru.yandex.practicum.mymarket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart")
@Data
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;
    private Long itemId;
    private Integer quantity;
    private Boolean isOrdered;
    private Long orderId;
    private LocalDateTime orderDate;

    @Transient
    private Item item;
}
