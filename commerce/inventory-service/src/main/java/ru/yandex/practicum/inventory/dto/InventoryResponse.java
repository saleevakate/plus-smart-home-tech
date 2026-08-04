package ru.yandex.practicum.inventory.dto;

public record InventoryResponse(
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}
