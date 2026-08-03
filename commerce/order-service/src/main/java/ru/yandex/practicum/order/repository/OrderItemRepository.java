package ru.yandex.practicum.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.order.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
