package com.example.demo.application.domain.order.query;

//根據 ID 查詢特定訂單
public record GetOrderQuery(String orderId) {}