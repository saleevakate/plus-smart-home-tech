package ru.yandex.practicum.inventory.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.inventory.dto.InventoryRequest;
import ru.yandex.practicum.inventory.dto.InventoryResponse;
import ru.yandex.practicum.inventory.model.Inventory;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        return new InventoryResponse(
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getQuantity() - inventory.getReservedQuantity()
        );
    }

    public Inventory toEntity(InventoryRequest request) {
        if (request == null) {
            return null;
        }

        return Inventory.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .reservedQuantity(0)
                .build();
    }

    public void updateEntity(Inventory inventory, InventoryRequest request) {
        if (request == null || inventory == null) {
            return;
        }

        if (request.quantity() != null) {
            inventory.setQuantity(request.quantity());
        }
    }

    public void updateReservedQuantity(Inventory inventory, Integer additionalReserved) {
        if (inventory == null || additionalReserved == null) {
            return;
        }
        inventory.setReservedQuantity(inventory.getReservedQuantity() + additionalReserved);
    }
}
