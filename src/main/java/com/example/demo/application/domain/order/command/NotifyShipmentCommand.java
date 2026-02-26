package com.example.demo.application.domain.order.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

//由 Saga 觸發：通知 Aggregate 付款已完成，可以準備出貨了
public record NotifyShipmentCommand(@TargetAggregateIdentifier String orderId) {}