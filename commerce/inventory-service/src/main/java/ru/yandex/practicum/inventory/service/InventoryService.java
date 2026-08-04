package ru.yandex.practicum.inventory.service;

import ru.yandex.practicum.inventory.dto.InventoryRequest;
import ru.yandex.practicum.inventory.dto.InventoryResponse;
import ru.yandex.practicum.inventory.dto.ReserveRequest;

import java.util.List;

public interface InventoryService {

    List<InventoryResponse> getAllInventory();

    InventoryResponse getInventoryByProductId(Long productId);

    InventoryResponse createInventory(InventoryRequest request);

    InventoryResponse updateQuantity(InventoryRequest request);

    InventoryResponse reserve(ReserveRequest request);

}
