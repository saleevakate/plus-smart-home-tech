package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.OrderRequest;
import ru.yandex.practicum.order.dto.OrderResponse;
import ru.yandex.practicum.order.exception.OrderNotFoundException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.*;
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
        for (Long productId : productQuantityMap.keySet()) {
            try {
                ProductDto product = productClient.getProductById(productId);

                if (!product.active()) {
                    throw new OrderProcessingException("Товар с ID " + productId + " снят с продажи");
                }

                productCache.put(productId, product);
                log.info("Получены данные товара: id={}, name={}", productId, product.name());

            } catch (FeignException.NotFound e) {
                throw new OrderProcessingException("Товар с ID " + productId + " не найден");
            } catch (FeignException e) {
                throw new OrderProcessingException("Ошибка получения данных товара: " + e.getMessage());
            }
        }

        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .status("PENDING")
                .totalPrice(BigDecimal.ZERO)
                .build();

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

        List<ReservedItem> reservedItems = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();

                ReserveRequest reserveRequest = new ReserveRequest(productId, quantity);

                try {
                    ReserveResponse response = inventoryClient.reserveStock(reserveRequest);
                    reservedItems.add(new ReservedItem(productId, quantity));
                    log.info("Товар {} зарезервирован в количестве {}", productId, quantity);

                } catch (FeignException.NotFound e) {
                    throw new OrderProcessingException("Складская запись для товара " + productId + " не найдена");
                } catch (FeignException.Conflict e) {
                    throw new OrderProcessingException("Недостаточно товара " + productId + " на складе");
                } catch (FeignException e) {
                    throw new OrderProcessingException("Ошибка резервирования товара " + productId);
                }
            }

            savedOrder.setStatus("CONFIRMED");
            Order confirmedOrder = orderRepository.save(savedOrder);
            log.info("Заказ {} подтвержден", confirmedOrder.getId());
            return orderMapper.toResponse(confirmedOrder);

        } catch (Exception e) {
            log.error("Ошибка создания заказа, выполняем откат резервов", e);
            releaseReservations(reservedItems);
            throw e;
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

    private static class ReservedItem {
        final Long productId;
        final Integer quantity;

        ReservedItem(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}
