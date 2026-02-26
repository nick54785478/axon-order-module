package com.example.demo.application.shared.query;

//查詢特定訂單的所有付款紀錄
public record GetOrderPaymentsQuery(String orderId) {
}