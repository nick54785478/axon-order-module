package com.example.demo.application.domain.order.event;

import java.math.BigDecimal;

/**
 * OrderCreatedEvent - 表示訂單已成功建立
 */
public record OrderCreatedEvent(
    String orderId,
    BigDecimal amount
) {}