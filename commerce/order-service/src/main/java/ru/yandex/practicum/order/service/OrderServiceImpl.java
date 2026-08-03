package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.OrderItemResponse;
import ru.yandex.practicum.order.dto.OrderRequest;
import ru.yandex.practicum.order.dto.OrderResponse;
import ru.yandex.practicum.order.exception.OrderNotFoundException;
import ru.yandex.practicum.order.model.Order;
import ru.yandex.practicum.order.model.OrderItem;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Создание заказа для клиента: {}", request.getCustomerEmail());

        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .totalPrice(BigDecimal.ZERO)
                .status("CREATED")
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(itemRequest.getProductId())
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .price(itemRequest.getPrice())
                    .build();

            order.getItems().add(item);
            total = total.add(itemRequest.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotalPrice(total);
        Order saved = orderRepository.save(order);
        log.info("Заказ создан с ID: {}, общая сумма: {}", saved.getId(), saved.getTotalPrice());

        return toResponse(saved);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        log.info("Поиск заказа по ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Заказ не найден с ID: " + id));
        return toResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        log.info("Запрос всех заказов");
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<OrderResponse> getOrdersByEmail(String email) {
        log.info("Поиск заказов по email: {}", email);
        return orderRepository.findByCustomerEmail(email).stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                itemResponses
        );
    }

}
