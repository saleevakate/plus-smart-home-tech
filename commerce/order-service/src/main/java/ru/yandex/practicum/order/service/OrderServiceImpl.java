package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.OrderRequest;
import ru.yandex.practicum.order.dto.OrderResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderNotFoundException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.*;
import ru.yandex.practicum.order.feign.client.InventoryClient;
import ru.yandex.practicum.order.feign.client.ProductClient;
import ru.yandex.practicum.order.feign.dto.ProductDto;
import ru.yandex.practicum.order.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.feign.dto.ReserveResponse;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.model.Order;
import ru.yandex.practicum.order.model.OrderItem;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    private ProductDto createDegradedProduct(Long productId) {
        return new ProductDto(
                productId,
                "Товар #" + productId + " (ожидает проверки)",
                "Данные товара временно недоступны",
                BigDecimal.ZERO,
                null,
                false
        );
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Начало создания заказа для клиента: {}", request.customerEmail());

        Map<Long, Integer> productQuantityMap = request.items().stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequest::productId,
                        Collectors.summingInt(OrderItemRequest::quantity)
                ));

        log.info("Группировка товаров: {}", productQuantityMap);

        Map<Long, ProductDto> productCache = new HashMap<>();
        boolean productServiceDegraded = false;

        for (Long productId : productQuantityMap.keySet()) {
            ServiceCallResult<ProductDto> result = getProductById(productId);

            switch (result) {
                case ServiceCallResult.Success<ProductDto>(ProductDto value) -> {
                    if (!value.active()) {
                        throw new OrderProcessingException("Товар с ID " + productId + " снят с продажи");
                    }
                    productCache.put(productId, value);
                    log.info("Получены данные товара: id={}, name={}", productId, value.name());
                }
                case ServiceCallResult.Failure<ProductDto>(String message) ->
                        throw new OrderProcessingException(message);
                case ServiceCallResult.Degraded<ProductDto>(String reason) -> {
                    log.warn("Сервис каталога недоступен: {}", reason);
                    productServiceDegraded = true;
                    productCache.put(productId, createDegradedProduct(productId));
                }
                default -> {
                }
            }
        }

        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .status(productServiceDegraded ? "PENDING_CONFIRMATION" : "PENDING")
                .totalPrice(BigDecimal.ZERO)
                .build();

        if (productServiceDegraded) {
            order.setStatusDetails("Заказ требует ручной проверки: данные каталога временно недоступны");
        }

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            ProductDto product = productCache.get(itemRequest.productId());

            OrderItem item = orderMapper.toItemEntity(order, itemRequest);
            item.setProductName(product.name());
            item.setPrice(product.price());

            order.getItems().add(item);
            total = total.add(product.price().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }
        order.setTotalPrice(total);

        Order savedOrder = orderRepository.save(order);
        log.info("Заказ сохранен с ID: {}, статус: {}", savedOrder.getId(), savedOrder.getStatus());

        if (productServiceDegraded) {
            log.info("Заказ сохранен с статусом PENDING_CONFIRMATION из-за недоступности каталога");
            return orderMapper.toResponse(savedOrder);
        }

        List<ReservedItem> reservedItems = new ArrayList<>();
        boolean inventoryServiceDegraded = false;

        try {
            label:
            for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();

                ReserveRequest reserveRequest = new ReserveRequest(productId, quantity);
                ServiceCallResult<ReserveResponse> result = reserveStock(reserveRequest);

                switch (result) {
                    case ServiceCallResult.Success<ReserveResponse> success:
                        reservedItems.add(new ReservedItem(productId, quantity));
                        log.info("Товар {} зарезервирован в количестве {}", productId, quantity);
                        break;
                    case ServiceCallResult.Failure<ReserveResponse>(String message):
                        throw new OrderProcessingException(message);
                    case ServiceCallResult.Degraded<ReserveResponse>(String reason):
                        log.warn("Сервис склада недоступен: {}", reason);
                        inventoryServiceDegraded = true;
                        break label;
                    default:
                        break;
                }
            }

            if (inventoryServiceDegraded) {
                log.info("Заказ переведен в статус PENDING_CONFIRMATION из-за недоступности склада");
                savedOrder.setStatus("PENDING_CONFIRMATION");
                savedOrder.setStatusDetails("Заказ требует ручной проверки: сервис склада временно недоступен");
                Order confirmedOrder = orderRepository.save(savedOrder);
                return orderMapper.toResponse(confirmedOrder);
            }

            savedOrder.setStatus("CONFIRMED");
            savedOrder.setStatusDetails(null);
            Order confirmedOrder = orderRepository.save(savedOrder);
            log.info("Заказ {} подтвержден", confirmedOrder.getId());
            return orderMapper.toResponse(confirmedOrder);

        } catch (OrderProcessingException e) {
            log.error("Ошибка создания заказа, выполняем откат резервов", e);
            releaseReservations(reservedItems);
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка создания заказа, выполняем откат резервов", e);
            releaseReservations(reservedItems);
            throw new OrderProcessingException("Ошибка создания заказа: " + e.getMessage());
        }
    }

    private ServiceCallResult<ProductDto> getProductById(Long productId) {
        try {
            ProductDto product = productClient.getProductById(productId);
            return new ServiceCallResult.Success<>(product);
        } catch (ProductServiceUnavailableException e) {
            return new ServiceCallResult.Degraded<>("Сервис каталога временно недоступен");
        } catch (FeignException.NotFound e) {
            return new ServiceCallResult.Failure<>("Товар с ID " + productId + " не найден");
        } catch (FeignException e) {
            return new ServiceCallResult.Failure<>("Ошибка получения данных товара");
        } catch (Exception e) {
            return new ServiceCallResult.Failure<>("Неизвестная ошибка при получении товара");
        }
    }

    private ServiceCallResult<ReserveResponse> reserveStock(ReserveRequest request) {
        try {
            ReserveResponse response = inventoryClient.reserveStock(request);
            return new ServiceCallResult.Success<>(response);
        } catch (InventoryServiceUnavailableException e) {
            return new ServiceCallResult.Degraded<>("Сервис склада временно недоступен");
        } catch (FeignException.NotFound e) {
            return new ServiceCallResult.Failure<>("Складская запись для товара " + request.productId() + " не найдена");
        } catch (FeignException.Conflict e) {
            return new ServiceCallResult.Failure<>("Недостаточно товара " + request.productId() + " на складе");
        } catch (FeignException e) {
            return new ServiceCallResult.Failure<>("Ошибка резервирования товара " + request.productId());
        } catch (Exception e) {
            return new ServiceCallResult.Failure<>("Неизвестная ошибка при резервировании товара");
        }
    }

    private void releaseReservations(List<ReservedItem> reservedItems) {
        if (reservedItems.isEmpty()) {
            log.info("Нет резервов для снятия");
            return;
        }

        log.info("Начинаем снятие резервов для {} позиций", reservedItems.size());
        for (ReservedItem item : reservedItems) {
            try {
                ReserveRequest releaseRequest = new ReserveRequest(item.productId, item.quantity);
                inventoryClient.releaseStock(releaseRequest);
                log.info("Снят резерв для товара {} в количестве {}", item.productId, item.quantity);
            } catch (Exception e) {
                log.error("Не удалось снять резерв для товара {}: {}", item.productId, e.getMessage());
            }
        }
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        log.info("Поиск заказа по ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Заказ не найден с ID: " + id));
        return orderMapper.toResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        log.info("Запрос всех заказов");
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public List<OrderResponse> getOrdersByEmail(String email) {
        log.info("Поиск заказов по email: {}", email);
        return orderRepository.findByCustomerEmail(email).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    private record ReservedItem(Long productId, Integer quantity) {
    }
}
