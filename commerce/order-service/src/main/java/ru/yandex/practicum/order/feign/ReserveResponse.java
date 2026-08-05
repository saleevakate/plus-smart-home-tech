package ru.yandex.practicum.order.feign;

public record ReserveResponse(
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}
