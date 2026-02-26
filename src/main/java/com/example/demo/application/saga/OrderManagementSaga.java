package com.example.demo.application.saga;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;

import com.example.demo.application.domain.order.command.CancelOrderCommand;
import com.example.demo.application.domain.order.command.NotifyShipmentCommand;
import com.example.demo.application.domain.order.event.OrderCancelledEvent;
import com.example.demo.application.domain.order.event.OrderCreatedEvent;
import com.example.demo.application.domain.order.event.OrderNotifiedEvent;
import com.example.demo.application.domain.payment.command.CancelPaymentCommand;
import com.example.demo.application.domain.payment.command.CreatePaymentCommand;
import com.example.demo.application.domain.payment.command.RefundPaymentCommand;
import com.example.demo.application.domain.payment.event.PaymentProcessedEvent;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderManagementSaga - 訂單管理流程協調者 *
 * <p>
 * 採用 Orchestration Saga 模式，負責協調 Order 與 Payment 聚合根之間的業務流程。 具備以下特性： 1.
 * 狀態持久化：成員變數會自動存入 saga_entry 供後續流程使用。 2. 支付超時管理：利用 DeadlineManager 實作支付倒數計時。 3.
 * 補償邏輯：當訂單取消時，確保支付紀錄同步關閉或退款。
 * </p>
 */
@Saga
@Slf4j
@NoArgsConstructor
@ProcessingGroup("order-saga")
public class OrderManagementSaga {

	/**
	 * 支付關聯 ID
	 */
	private String paymentId;

	/**
	 * 訂單金額，用於後續退款補償
	 */
	private BigDecimal amount;

	/**
	 * 付款完成旗標
	 */
	private boolean paymentCompleted = false;

	/**
	 * 超時任務 ID，用於支付成功後取消任務
	 */
	private String paymentDeadlineId;

	/**
	 * 步驟 1：訂單建立 (Saga 起點) * @param event 訂單建立事件
	 * 
	 * @param commandGateway  用於發送 CreatePaymentCommand
	 * @param deadlineManager 用於預約支付超時任務
	 */
	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderCreatedEvent event, CommandGateway commandGateway, DeadlineManager deadlineManager) {
		log.info("[Saga] 訂單 {} 建立，開始 10 分鐘支付倒數。", event.orderId());

		this.amount = event.amount();
		this.paymentId = UUID.randomUUID().toString();

		// 建立與 paymentId 的關聯，以便此 Saga 能接收來自 Payment 模組的事件
		SagaLifecycle.associateWith("paymentId", this.paymentId);

		// 預約支付超時任務 (10 分鐘)
		// 將 event 作為 Payload 傳遞給 DeadlineHandler
		this.paymentDeadlineId = deadlineManager.schedule(Duration.ofMinutes(10), "payment-deadline", event);

		// 建立付款紀錄 (狀態為 CREATED)，等待使用者手動執行支付 API
		commandGateway.send(new CreatePaymentCommand(this.paymentId, event.orderId(), event.amount()))
				.exceptionally(ex -> {
					log.error("[Saga] 建立付款紀錄失敗，執行補償：取消訂單。原因: {}", ex.getMessage());
					commandGateway.send(new CancelOrderCommand(event.orderId()));
					return null;
				});
	}

	/**
	 * 步驟 2：外部觸發支付成功 * @param event 支付完成事件 (由使用者調用支付 API 觸發)
	 * 
	 * @param commandGateway  用於發送出貨通知
	 * @param deadlineManager 用於取消支付超時任務
	 */
	@SagaEventHandler(associationProperty = "paymentId")
	public void on(PaymentProcessedEvent event, CommandGateway commandGateway, DeadlineManager deadlineManager) {
		log.info("[Saga] 偵測到支付成功！取消倒數並通知出貨。OrderId: {}", event.orderId());

		this.paymentCompleted = true;

		// 取消之前預約的支付超時任務
		if (this.paymentDeadlineId != null) {
			deadlineManager.cancelSchedule("payment-deadline", this.paymentDeadlineId);
			this.paymentDeadlineId = null;
		}

		// 流程推進：通知出貨 (進入 NOTIFIED 狀態)
		commandGateway.send(new NotifyShipmentCommand(event.orderId())).exceptionally(ex -> {
			log.error("[Saga] 通知出貨失敗，執行退款補償。");
			commandGateway.send(new RefundPaymentCommand(event.paymentId(), event.orderId(), event.amount()));
			return null;
		});
	}

	/**
	 * 支付超時處理器 (Deadline Handler) * @param event 原始建立訂單事件
	 * 
	 * @param commandGateway 用於執行超時取消
	 */
	@DeadlineHandler(deadlineName = "payment-deadline")
	public void handlePaymentTimeout(OrderCreatedEvent event, CommandGateway commandGateway) {
		log.warn("[Saga] 支付超時時間到！自動發送取消訂單指令。OrderId: {}", event.orderId());
		// 觸發取消訂單，這會導致 Order 產生 OrderCancelledEvent
		commandGateway.send(new CancelOrderCommand(event.orderId()));
	}

	/**
	 * 步驟 5：訂單取消處理 (手動或超時觸發) * @param event 訂單取消事件
	 * 
	 * @param commandGateway 用於執行支付關閉或退款
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderCancelledEvent event, CommandGateway commandGateway) {
		log.info("[Saga] 收到 OrderCancelledEvent，同步更新支付狀態。OrderId: {}", event.orderId());

		// 防禦邏輯 1：如果訂單未付錢即取消，同步關閉支付紀錄，防止過期支付
		if (!this.paymentCompleted && this.paymentId != null) {
			log.info("[Saga] 關閉支付紀錄: {}", this.paymentId);
			commandGateway.send(new CancelPaymentCommand(this.paymentId, event.orderId()));
		}

		// 防禦邏輯 2：如果訂單已付錢但被取消，則執行退款
		if (this.paymentCompleted) {
			log.warn("[Saga] 訂單已付款但被取消，執行退款流程。PaymentId: {}", this.paymentId);
			commandGateway.send(new RefundPaymentCommand(this.paymentId, event.orderId(), this.amount));
		}

		// 流程結束
		SagaLifecycle.end();
	}

	/**
	 * 流程終點：成功通知出貨
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderNotifiedEvent event) {
		log.info("[Saga] 訂單 {} 已成功通知出貨，流程結束。", event.orderId());
		SagaLifecycle.end();
	}
}