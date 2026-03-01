package com.example.demo.application.domain.order.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.modelling.command.TargetAggregateVersion;

/**
 * ReturnOrderCommand - 發起退貨指令
 */
public record ReturnOrderCommand(
    @TargetAggregateIdentifier String orderId,
    @TargetAggregateVersion Long version, // 樂觀鎖防護
    String reason
) {}
