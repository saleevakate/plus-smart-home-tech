package ru.yandex.practicum.order.service;

import ru.yandex.practicum.order.dto.OrderRequest;
import ru.yandex.practicum.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getAllOrders();

    List<OrderResponse> getOrdersByEmail(String email);

}
