package ru.yandex.practicum.order.feign.dto;

public record ReserveResponse(
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}
