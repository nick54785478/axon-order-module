package com.example.demo.application.domain.order.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.modelling.command.TargetAggregateVersion;

/**
 * ConfirmOrderShipmentCommand - 手動確認出貨指令
 */
public record ConfirmOrderShipmentCommand(@TargetAggregateIdentifier String orderId,
		@TargetAggregateVersion Long version) {
}