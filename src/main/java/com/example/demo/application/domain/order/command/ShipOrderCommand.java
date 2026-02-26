package com.example.demo.application.domain.order.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * ShipOrderCommand
 */
public record ShipOrderCommand(@TargetAggregateIdentifier String orderId) {
}