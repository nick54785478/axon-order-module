package com.example.demo.application.saga;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;

import com.example.demo.application.domain.order.aggregate.vo.OrderItem;
import com.example.demo.application.domain.order.command.CancelOrderCommand;
import com.example.demo.application.domain.order.command.NotifyShipmentCommand;
import com.example.demo.application.domain.order.event.OrderCancelledEvent;
import com.example.demo.application.domain.order.event.OrderCreatedEvent;
import com.example.demo.application.domain.order.event.OrderNotifiedEvent;
import com.example.demo.application.domain.payment.command.CancelPaymentCommand;
import com.example.demo.application.domain.payment.command.CreatePaymentCommand;
import com.example.demo.application.domain.payment.command.RefundPaymentCommand;
import com.example.demo.application.domain.payment.event.PaymentProcessedEvent;
import com.example.demo.application.domain.product.command.ReduceStockCommand;
import com.example.demo.application.domain.product.event.StockReducedEvent;
import com.example.demo.application.shared.command.AddStockCommand;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderManagementSaga - 訂單管理流程協調者
 *
 * <p>
 * 本類別採用 Orchestration Saga 模式，負責協調 Order、Product 與 Payment 聚合根之間的分散式事務。
 * 核心流程遵循「資源預留 -> 執行支付 -> 出貨確認」的階段，並具備完整的逆向補償機制。
 * </p>
 *
 * <h3>核心機制：</h3>
 * <ul>
 * <li><b>冪等性防護：</b> 透過 {@code isCancelling} 標記與 {@code synchronized}
 * 鎖，防止併發操作下的重複取消。</li>
 * <li><b>精確補償 (TCC 理念)：</b> 利用 {@code reservedItems}
 * 記錄成功扣減的資源，確保補償時僅還原受影響的庫存。</li>
 * <li><b>生命週期管理：</b> 透過 {@code SagaLifecycle.end()} 確保事務在終點（成功或失敗補償後）正確關閉。</li>
 * </ul>
 */
@Saga
@Slf4j
@NoArgsConstructor
@ProcessingGroup("order-saga")
public class OrderManagementSaga {

	private String paymentId;
	private BigDecimal amount;
	private String orderId;
	private boolean paymentCompleted = false;
	private String paymentDeadlineId;

	/**
	 * 併發防護標記：確保在多品項扣減失敗或超時併發時，取消指令僅發送一次。
	 */
	private boolean isCancelling = false;

	/**
	 * 已保留品項清單：用於補償流程。 僅記錄 {@code StockReducedEvent} 成功確認的品項，實現「只還原扣掉的」精確補償。
	 */
	private List<OrderItem> reservedItems = new ArrayList<>();

	/**
	 * 步驟 1：訂單建立 -> 發起庫存扣減
	 * <p>
	 * Saga 起點。此階段執行「樂觀預留」，嘗試對所有品項執行庫存扣減。
	 * </p>
	 * * @param event 訂單建立事件
	 * 
	 * @param commandGateway  用於發送非同步指令
	 * @param deadlineManager 用於預約支付超時任務
	 */
	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderCreatedEvent event, CommandGateway commandGateway, DeadlineManager deadlineManager) {
		log.info("[Saga] 訂單 {} 已建立。開始請求庫存預留 (品項數: {})。", event.orderId(), event.items().size());

		this.orderId = event.orderId();
		this.amount = event.amount();

		// 預約 10 分鐘支付超時任務，逾期將觸發 handlePaymentTimeout
		this.paymentDeadlineId = deadlineManager.schedule(Duration.ofMinutes(10), "payment-deadline", event);

		// 併發發送各品項扣減指令
		for (OrderItem item : event.items()) {
			commandGateway.send(new ReduceStockCommand(item.productId(), this.orderId, item.quantity()))
					.exceptionally(ex -> {
						// 異常處理：若任一品項失敗，執行原子化的取消標記與指令發送
						synchronized (this) {
							if (!isCancelling) {
								isCancelling = true;
								log.error("[Saga] 產品 {} 庫存扣減失敗 (原因: {})。啟動訂單取消流程。", item.productId(), ex.getMessage());
								commandGateway.send(new CancelOrderCommand(this.orderId));
							}
						}
						return null;
					});
		}
	}

	/**
	 * 步驟 2：庫存扣減成功
	 * <p>
	 * 當 Product 聚合根確認庫存充足並扣除後，記錄該品項，並在符合條件下開啟支付流程。
	 * </p>
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(StockReducedEvent event, CommandGateway commandGateway) {
		log.info("[Saga] 產品 {} 庫存預留成功 (OrderId: {})。", event.productId(), event.orderId());

		// 將成功扣減的品項加入補償清單
		this.reservedItems.add(new OrderItem(event.productId(), event.quantity(), BigDecimal.ZERO));

		// 支付發起檢查：
		// 1. 若尚未建立過支付紀錄 (避免多品項重複觸發)
		// 2. 且目前不在取消流程中
		synchronized (this) {
			if (this.paymentId == null && !isCancelling) {
				this.paymentId = UUID.randomUUID().toString();
				SagaLifecycle.associateWith("paymentId", this.paymentId);

				log.info("[Saga] 所有初始條件達成，建立支付請求: {}", this.paymentId);
				commandGateway.send(new CreatePaymentCommand(this.paymentId, this.orderId, this.amount));
			}
		}
	}

	/**
	 * 步驟 3：支付完成
	 * <p>
	 * 支付模組成功處理扣款後，取消超時倒數並通知出貨端。
	 * </p>
	 */
	@SagaEventHandler(associationProperty = "paymentId")
	public void on(PaymentProcessedEvent event, CommandGateway commandGateway, DeadlineManager deadlineManager) {
		log.info("[Saga] 支付成功確認！關閉超時監控。OrderId: {}", event.orderId());
		this.paymentCompleted = true;

		// 取消支付超時任務，避免訂單在付款後被意外取消
		if (this.paymentDeadlineId != null) {
			deadlineManager.cancelSchedule("payment-deadline", this.paymentDeadlineId);
		}

		// 通知出貨模組 (Order Aggregate 狀態機轉跳)
		commandGateway.send(new NotifyShipmentCommand(event.orderId()));
	}

	/**
	 * 步驟 4：補償機制 (由 OrderCancelledEvent 觸發)
	 * <p>
	 * 本方法是 Saga 的防線，負責清理所有已佔用的資源（庫存、金額）。
	 * </p>
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderCancelledEvent event, CommandGateway commandGateway) {
		log.warn("[Saga] 訂單 {} 進入取消程序，開始執行資源回收 (Compensation)。", event.orderId());

		this.isCancelling = true;

		// 1. 庫存補償：還原所有在 reservedItems 中記錄的成功扣減
		if (!reservedItems.isEmpty()) {
			for (OrderItem item : reservedItems) {
				log.info("[Saga] 補償：還原產品 {} 的庫存 {} 單位。", item.productId(), item.quantity());
				commandGateway.send(new AddStockCommand(item.productId(), this.orderId, item.quantity()));
			}
		}

		// 2. 支付補償：根據支付狀態執行「退款」或「關閉紀錄」
		if (this.paymentId != null) {
			if (this.paymentCompleted) {
				log.info("[Saga] 補償：訂單已支付，發起退款請求 (PaymentId: {})。", this.paymentId);
				commandGateway.send(new RefundPaymentCommand(this.paymentId, this.orderId, this.amount));
			} else {
				log.info("[Saga] 補償：關閉未支付之紀錄 (PaymentId: {})。", this.paymentId);
				commandGateway.send(new CancelPaymentCommand(this.paymentId, this.orderId));
			}
		}

		// 事務結束：從記憶體中清除此 Saga 實例
		SagaLifecycle.end();
	}

	/**
	 * 支付超時處理器
	 * <p>
	 * 當用戶未在預定時間內完成支付時由 DeadlineManager 觸發。
	 * </p>
	 */
	@DeadlineHandler(deadlineName = "payment-deadline")
	public void handlePaymentTimeout(OrderCreatedEvent event, CommandGateway commandGateway) {
		synchronized (this) {
			// 防禦校驗：僅在未支付且未在取消中的狀態下執行超時取消
			if (!isCancelling && !paymentCompleted) {
				this.isCancelling = true;
				log.warn("[Saga] 偵測到支付超時，系統自動發起訂單取消。OrderId: {}", event.orderId());
				commandGateway.send(new CancelOrderCommand(this.orderId));
			}
		}
	}

	/**
	 * 流程終點：通知出貨成功
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderNotifiedEvent event) {
		log.info("[Saga] 訂單 {} 流程圓滿結束，歸檔 Saga。", event.orderId());
		SagaLifecycle.end();
	}
}