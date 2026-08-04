package ru.yandex.practicum.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull @Positive Long productId,
        @NotNull String productName,
        @NotNull @Positive Integer quantity,
        @NotNull @Positive BigDecimal price
) {
}
