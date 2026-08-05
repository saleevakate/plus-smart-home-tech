package ru.yandex.practicum.order.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.OrderItemResponse;
import ru.yandex.practicum.order.dto.OrderRequest;
import ru.yandex.practicum.order.dto.OrderResponse;
import ru.yandex.practicum.order.model.Order;
import ru.yandex.practicum.order.model.OrderItem;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                itemResponses,
                order.getStatusDetails()
        );
    }

    public Order toEntity(OrderRequest request) {
        if (request == null) {
            return null;
        }

        return Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .totalPrice(BigDecimal.ZERO)
                .status("CREATED")
                .build();
    }

    public OrderItem toItemEntity(Order order, OrderItemRequest itemRequest) {
        if (itemRequest == null || order == null) {
            return null;
        }

        return OrderItem.builder()
                .order(order)
                .productId(itemRequest.productId())
                .quantity(itemRequest.quantity())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}
