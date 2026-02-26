package com.example.demo.application.service;

import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.order.command.CancelOrderCommand;
import com.example.demo.application.domain.order.command.ConfirmOrderShipmentCommand;
import com.example.demo.application.domain.order.command.CreateOrderCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class OrderCommandService {

	private final CommandGateway commandGateway;

	/**
	 * 建立訂單並通知外部人員出貨
	 * 
	 * @param command {@link CreateOrderCommand}
	 */
	public void createOrder(CreateOrderCommand command) {
		log.info("發送 CreateOrderCommand，orderId: {}", command.orderId());
		commandGateway.send(command);
	}

	/**
	 * 手動確認出貨
	 * 
	 * @param command {@link ConfirmOrderShipmentCommand}
	 */
	public void confirmShipment(ConfirmOrderShipmentCommand command) {
		log.info("收到出貨確認請求: {}", command.orderId());
		commandGateway.send(command);
	}

	/**
	 * 取消訂單指令發送
	 * 
	 * @param orderId 訂單 ID
	 */
	public CompletableFuture<Void> cancelOrder(String orderId) {
		log.info("[Service] 發送 CancelOrderCommand，orderId: {}", orderId);
		// 發送指令後回傳 CompletableFuture 以便 Controller 處理非同步結果
		return commandGateway.send(new CancelOrderCommand(orderId));
	}


}
