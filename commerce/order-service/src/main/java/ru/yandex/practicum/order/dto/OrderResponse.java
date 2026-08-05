package ru.yandex.practicum.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String customerEmail,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        String statusDetails
) {
}
