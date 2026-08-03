package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryRequest;
import ru.yandex.practicum.inventory.dto.InventoryResponse;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.InventoryNotFoundException;
import ru.yandex.practicum.inventory.model.Inventory;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public List<InventoryResponse> getAllInventory() {
        log.info("Запрос всех складских записей");
        return inventoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public InventoryResponse getInventoryByProductId(Long productId) {
        log.info("Запрос остатков по товару: {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Складская запись не найдена для товара: " + productId));
        return toResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {
        log.info("Создание складской записи для товара: {}", request.getProductId());

        inventoryRepository.findByProductId(request.getProductId())
                .ifPresent(inv -> {
                    log.warn("Складская запись для товара {} уже существует", request.getProductId());
                    throw new RuntimeException(
                            "Складская запись для товара " + request.getProductId() + " уже существует");
                });

        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .reservedQuantity(0)
                .build();

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Складская запись для товара {} создана, количество: {}", request.getProductId(), saved.getQuantity());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryResponse updateQuantity(InventoryRequest request) {
        log.info("Обновление количества товара: {}, новое количество: {}", request.getProductId(), request.getQuantity());

        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Складская запись не найдена для товара: " + request.getProductId()));

        int reserved = inventory.getReservedQuantity();
        int total = request.getQuantity();

        if (total < reserved) {
            log.warn("Нельзя уменьшить количество ниже зарезервированного. Товар: {}, всего: {}, зарезервировано: {}",
                    request.getProductId(), total, reserved);
            throw new RuntimeException(
                    "Нельзя уменьшить количество ниже зарезервированного (зарезервировано: " + reserved + ")");
        }

        inventory.setQuantity(total);
        Inventory updated = inventoryRepository.save(inventory);
        log.info("Количество товара {} обновлено до {}", request.getProductId(), updated.getQuantity());
        return toResponse(updated);
    }

    @Override
    @Transactional
    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public InventoryResponse reserve(ReserveRequest request) {
        log.info("Резервирование товара: {}, количество: {}", request.getProductId(), request.getQuantity());

        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Складская запись не найдена для товара: " + request.getProductId()));

        int available = inventory.getQuantity() - inventory.getReservedQuantity();

        if (available < request.getQuantity()) {
            log.warn("Недостаточно товара на складе. Товар: {}, доступно: {}, запрошено: {}",
                    request.getProductId(), available, request.getQuantity());
            throw new InsufficientStockException(
                    "Недостаточно товара на складе. Доступно: " + available + ", запрошено: " + request.getQuantity());
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
        Inventory updated = inventoryRepository.save(inventory);

        log.info("Товар {} зарезервирован. Зарезервировано: {}, доступно: {}",
                request.getProductId(),
                updated.getReservedQuantity(),
                updated.getQuantity() - updated.getReservedQuantity());

        return toResponse(updated);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getQuantity() - inventory.getReservedQuantity()
        );
    }
}
