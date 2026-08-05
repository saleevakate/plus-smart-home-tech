package ru.yandex.practicum.order.feign;

public record InventoryResponse(
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}
