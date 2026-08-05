package ru.yandex.practicum.order.feign.dto;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Boolean active
) {
}
