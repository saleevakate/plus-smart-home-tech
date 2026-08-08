package ru.yandex.practicum.order.feign.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive Integer quantity
) {
}
