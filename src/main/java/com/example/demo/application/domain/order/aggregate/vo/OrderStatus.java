package com.example.demo.application.domain.order.aggregate.vo;

public enum OrderStatus {
	CREATED, // 訂單已建立
	NOTIFIED, // 已通知出貨 (已付款)
	SHIPPED, // 已出貨 (人工觸發)
	CANCELLED // 已取消
}