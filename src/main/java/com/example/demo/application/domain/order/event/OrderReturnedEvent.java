package com.example.demo.application.domain.order.event;

import java.util.List;

import com.example.demo.application.domain.order.aggregate.vo.OrderItem;

/**
 * OrderReturnedEvent - 訂單已進入退貨程序事件
 */
public record OrderReturnedEvent(String orderId, List<OrderItem> items, // 用於還原庫存
		String reason) {
}
