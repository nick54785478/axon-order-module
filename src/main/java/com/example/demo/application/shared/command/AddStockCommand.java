package com.example.demo.application.shared.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * AddStockCommand - 增加/還原庫存指令
 * <p>
 * 當訂單取消或支付失敗時，由 Saga 發起補償請求。
 * </p>
 */
public record AddStockCommand(@TargetAggregateIdentifier String productId, String orderId, Integer quantity) {
}