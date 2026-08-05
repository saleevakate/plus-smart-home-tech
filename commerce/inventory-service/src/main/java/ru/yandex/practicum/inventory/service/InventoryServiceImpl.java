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
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.model.Inventory;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public List<InventoryResponse> getAllInventory() {
        log.info("Запрос всех складских записей");
        return inventoryRepository.findAll().stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    public InventoryResponse getInventoryByProductId(Long productId) {
        log.info("Запрос остатков по товару: {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Складская запись не найдена для товара: " + productId));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {
        log.info("Создание складской записи для товара: {}", request.productId());

        inventoryRepository.findByProductId(request.productId())
                .ifPresent(inv -> {
                    log.warn("Складская запись для товара {} уже существует", request.productId());
                    throw new RuntimeException(
                            "Складская запись для товара " + request.productId() + " уже существует");
                });

        Inventory inventory = inventoryMapper.toEntity(request);
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Складская запись для товара {} создана, количество: {}", request.productId(), saved.getQuantity());
        return inventoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryResponse updateQuantity(InventoryRequest request) {
        log.info("Обновление количества товара: {}, новое количество: {}", request.productId(), request.quantity());

        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Складская запись не найдена для товара: " + request.productId()));

        int reserved = inventory.getReservedQuantity();
        int total = request.quantity();

        if (total < reserved) {
            log.warn("Нельзя уменьшить количество ниже зарезервированного. Товар: {}, всего: {}, зарезервировано: {}",
                    request.productId(), total, reserved);
            throw new RuntimeException(
                    "Нельзя уменьшить количество ниже зарезервированного (зарезервировано: " + reserved + ")");
        }

        inventoryMapper.updateEntity(inventory, request);
        Inventory updated = inventoryRepository.save(inventory);
        log.info("Количество товара {} обновлено до {}", request.productId(), updated.getQuantity());
        return inventoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public InventoryResponse reserve(ReserveRequest request) {
        log.info("Резервирование товара: {}, количество: {}", request.productId(), request.quantity());

        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Складская запись не найдена для товара: " + request.productId()));

        int available = inventory.getQuantity() - inventory.getReservedQuantity();

        if (available < request.quantity()) {
            log.warn("Недостаточно товара на складе. Товар: {}, доступно: {}, запрошено: {}",
                    request.productId(), available, request.quantity());
            throw new InsufficientStockException(
                    "Недостаточно товара на складе. Доступно: " + available + ", запрошено: " + request.quantity());
        }

        inventoryMapper.updateReservedQuantity(inventory, request.quantity());
        Inventory updated = inventoryRepository.save(inventory);

        log.info("Товар {} зарезервирован. Зарезервировано: {}, доступно: {}",
                request.productId(),
                updated.getReservedQuantity(),
                updated.getQuantity() - updated.getReservedQuantity());

        return inventoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public InventoryResponse release(ReserveRequest request) {
        log.info("Снятие резерва товара: {}, количество: {}", request.productId(), request.quantity());

        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Складская запись не найдена для товара: " + request.productId()));

        int reserved = inventory.getReservedQuantity();
        if (reserved < request.quantity()) {
            log.warn("Нельзя снять больше, чем зарезервировано. Товар: {}, зарезервировано: {}, запрошено: {}",
                    request.productId(), reserved, request.quantity());
            throw new IllegalArgumentException(
                    "Нельзя снять больше, чем зарезервировано. Зарезервировано: " + reserved + ", запрошено: " + request.quantity());
        }

        inventory.setReservedQuantity(reserved - request.quantity());
        Inventory updated = inventoryRepository.save(inventory);

        log.info("Резерв товара {} снят. Зарезервировано: {}, доступно: {}",
                request.productId(),
                updated.getReservedQuantity(),
                updated.getQuantity() - updated.getReservedQuantity());

        return inventoryMapper.toResponse(updated);
    }
}
