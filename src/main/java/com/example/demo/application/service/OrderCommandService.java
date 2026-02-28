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
	 * 建立訂單並啟動後續 Saga 流程
	 * 
	 * @param command 包含品項與金額的建立指令
	 * @return CompletableFuture 包含產生的 OrderId
	 */
	public CompletableFuture<String> createOrder(CreateOrderCommand command) {
		log.info("[Service] 發送 CreateOrderCommand，orderId: {}, 品項數: {}, 金額: {}", command.orderId(),
				command.items().size(), command.amount());

		// send() 會回傳 CompletableFuture，讓 Controller 可以透過 .thenApply() 或
		// .exceptionally() 處理
		return commandGateway.send(command);
	}

	/**
	 * 手動確認出貨 (由人員操作 API 觸發)
	 * 
	 * @param command 出貨確認指令
	 * @return CompletableFuture 執行結果
	 */
	public CompletableFuture<Void> confirmShipment(ConfirmOrderShipmentCommand command) {
		log.info("[Service] 收到出貨確認請求，發送指令: {}", command.orderId());

		// 這裡回傳 Future，確保 Aggregate 若拋出 "狀態不正確" 的異常能被 Controller 捕獲
		return commandGateway.send(command);
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
