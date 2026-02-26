package com.example.demo.application.domain.order.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * CancelOrderCommand - 用於取消訂單
 */
public record CancelOrderCommand(@TargetAggregateIdentifier String orderId) {
}