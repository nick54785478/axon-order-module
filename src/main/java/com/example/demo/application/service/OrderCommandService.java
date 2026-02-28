package com.example.demo.application.service;

import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.order.aggregate.Order;
import com.example.demo.application.domain.order.command.CancelOrderCommand;
import com.example.demo.application.domain.order.command.ConfirmOrderShipmentCommand;
import com.example.demo.application.domain.order.command.CreateOrderCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderCommandService - 訂單指令應用服務
 *
 * <p>
 * 本服務負責管理訂單的生命週期操作。 它是「寫入端」的核心入口，負責啟動訂單的建立、出貨以及取消等流程。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class OrderCommandService {

	private final CommandGateway commandGateway;

	/**
	 * 建立新訂單 *
	 * <p>
	 * 此為訂單生命週期的起點。發送指令後，{@link Order} 聚合根會執行金額校驗。
	 * </p>
	 * <p>
	 * 成功建立後，通常會觸發後續的 Saga 流程進行自動化處理。
	 * </p>
	 * * @param command 包含品項清單、預期金額與 ID 的建立指令
	 * 
	 * @return CompletableFuture&lt;String&gt; 成功後回傳建立的 OrderId
	 */
	public CompletableFuture<String> createOrder(CreateOrderCommand command) {
		log.info("[Order Service] 啟動訂單建立程序: orderId={}, 品項數: {}", command.orderId(), command.items().size());

		// send() 回傳 Future，讓 API 層能攔截來自 Aggregate 的校驗異常 (如金額不符)
		return commandGateway.send(command);
	}

	/**
	 * 手動確認訂單出貨 *
	 * <p>
	 * 由管理人員透過 API 手動觸發。此操作僅在訂單處於「已通知出貨 (NOTIFIED)」狀態下有效。
	 * </p>
	 * * @param command 出貨確認指令
	 * 
	 * @return CompletableFuture&lt;Void&gt;
	 */
	public CompletableFuture<Void> confirmShipment(ConfirmOrderShipmentCommand command) {
		log.info("[Order Service] 收到手動出貨請求，目標訂單: {}", command.orderId());

		return commandGateway.send(command);
	}

	/**
	 * 撤銷/取消特定訂單 *
	 * <p>
	 * 發送取消指令。若訂單已進入出貨階段，Aggregate 內部會拋出 IllegalStateException 阻止取消。
	 * </p>
	 * * @param orderId 欲取消的訂單 ID
	 * 
	 * @return CompletableFuture&lt;Void&gt;
	 */
	public CompletableFuture<Void> cancelOrder(String orderId) {
		log.info("[Order Service] 執行訂單取消指令: {}", orderId);

		return commandGateway.send(new CancelOrderCommand(orderId));
	}
}
