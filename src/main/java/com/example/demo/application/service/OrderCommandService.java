package com.example.demo.application.service;

import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.order.aggregate.Order;
import com.example.demo.application.domain.order.command.CancelOrderCommand;
import com.example.demo.application.domain.order.command.ConfirmOrderShipmentCommand;
import com.example.demo.application.domain.order.command.CreateOrderCommand;
import com.example.demo.application.domain.order.command.ReturnOrderCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderCommandService - 訂單指令應用服務
 *
 * <p>
 * 本服務作為六角形架構中的「入站適配器（Inbound Adapter）」與領域模型之間的橋樑。 負責將 API
 * 層的請求封裝為領域指令（Commands），並透過 {@link CommandGateway} 發送至 {@link Order} 聚合根。
 * </p>
 *
 * <h3>核心職責：</h3>
 * <ul>
 * <li><b>意圖轉發：</b> 將複雜的業務動作轉換為原子化的指令。</li>
 * <li><b>併發控制：</b> 傳遞前端提供的 {@code version}，由框架執行樂觀鎖檢查。</li>
 * <li><b>異常傳遞：</b> 透過 {@link CompletableFuture} 異步處理結果，允許 API
 * 層捕獲領域層拋出的校驗異常。</li>
 * </ul>
 */
@Slf4j
@Service
@AllArgsConstructor
public class OrderCommandService {

	private final CommandGateway commandGateway;

	/**
	 * 建立新訂單
	 * <p>
	 * 此為訂單生命週期的起點。發送指令後，{@link Order} 聚合根會執行品項驗證與金額一致性校驗。 成功建立後將發布
	 * {@code OrderCreatedEvent}，進而觸發 Saga 流程進行後續的庫存預留與支付建立。
	 * </p>
	 * 
	 * @param command 包含品項明細、總金額與預定 ID 的建立指令
	 * @return {@link CompletableFuture} 成功後回傳建立的訂單 ID (orderId)
	 */
	public CompletableFuture<String> createOrder(CreateOrderCommand command) {
		log.info("[Order Service] 啟動訂單建立程序: orderId={}, 品項數: {}", command.orderId(), command.items().size());

		// 透過 Gateway 發送指令，Aggregate 的建構子將被觸發
		return commandGateway.send(command);
	}

	/**
	 * 手動確認訂單出貨
	 * <p>
	 * 此操作通常由倉管人員操作後觸發。 <b>業務規則：</b> 訂單必須處於「已通知出貨 (NOTIFIED)」狀態。 指令中包含
	 * {@code version} 以防止管理員在過時的訂單狀態下執行出貨。
	 * </p>
	 * 
	 * @param command 出貨確認指令，包含訂單 ID 與版本號
	 * @return {@link CompletableFuture} 用於追蹤操作是否成功執行
	 */
	public CompletableFuture<Void> confirmShipment(ConfirmOrderShipmentCommand command) {
		log.info("[Order Service] 收到手動出貨請求，目標訂單: {}, 版本: {}", command.orderId(), command.version());

		return commandGateway.send(command);
	}

	/**
	 * 撤銷/取消特定訂單
	 * <p>
	 * 執行訂單取消動作。此指令可能來自用戶主動取消或系統補償流程。 <b>安全性防護：</b>
	 * 若訂單已進入出貨階段（SHIPPED），聚合根內部的狀態機將攔截並拋出異常。
	 * </p>
	 * 
	 * @param command 包含訂單 ID 與版本號的取消指令
	 * @return {@link CompletableFuture} 處理結果
	 */
	public CompletableFuture<Void> cancelOrder(CancelOrderCommand command) {
		log.info("[Order Service] 執行訂單取消指令: {}, 版本: {}", command.orderId(), command.version());

		return commandGateway.send(command);
	}

	/**
	 * 申請訂單退貨
	 * <p>
	 * 針對已出貨（SHIPPED）之訂單發起逆向物流程序。 成功執行後會發布 {@code OrderReturnedEvent}，並連動 Saga
	 * 啟動退款與庫存回撥流程。
	 * </p>
	 * 
	 * @param command 退貨指令，包含訂單 ID、版本號與退貨原因
	 * @return {@link CompletableFuture} 用於確認退貨申請是否已受理
	 */
	public CompletableFuture<Void> returnOrder(ReturnOrderCommand command) {
		log.info("[Order Service] 接收退貨請求: OrderId={}, 原因={}", command.orderId(), command.reason());

		return commandGateway.send(command);
	}
}