package com.example.demo.application.domain.order.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

//由新 API 觸發：代表物流已完成實體出貨
public record ConfirmOrderShipmentCommand(@TargetAggregateIdentifier String orderId) {}