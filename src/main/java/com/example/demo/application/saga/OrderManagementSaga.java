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
import com.example.demo.application.domain.order.event.OrderReturnedEvent;
import com.example.demo.application.domain.payment.aggregate.Payment;
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
 * 整合了「正向訂單流」、「超時自動保護」以及「逆向資源回收 (取消與退貨)」。
 * </p>
 *
 * <h3>架構核心：</h3>
 * <ul>
 * <li><b>冪等性與併發防護：</b> 透過 {@code isCancelling} 標記與 {@code synchronized}
 * 鎖，防止多重失敗或超時導致的重複指令。</li>
 * <li><b>樂觀鎖繞過策略：</b> 由 Saga 發起的系統級補償指令將版本號設為
 * {@code null}，確保任務能跨版本強制執行，保障最終一致性。</li>
 * <li><b>統一補償模型：</b> 透過 {@code performCompensation}
 * 方法，將「取消」與「退貨」產生的物理補償邏輯（還原庫存、退款）高度抽象化。</li>
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
	 * 併發防護標記：防止在極短時間內重複啟動逆向流程（如：超時補償與手動取消同時觸發）。
	 */
	private boolean isCancelling = false;

	/**
	 * 精確補償清單：僅記錄成功扣減的品項，實現「精確還原」而非全量還原。
	 */
	private List<OrderItem> reservedItems = new ArrayList<>();

	// ##### 1. 訂單發起與資源預留 (Order Initiation) #####

	/**
	 * 步驟 1：訂單建立 -> 請求庫存扣減
	 * <p>
	 * 啟動 Saga 事務，預約支付超時任務，並對所有品項發起非同步庫存預留請求。
	 * </p>
	 */
	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderCreatedEvent event, CommandGateway commandGateway, DeadlineManager deadlineManager) {
		log.info("[Saga] 訂單 {} 啟動。發起庫存預留與超時監控。", event.orderId());

		this.orderId = event.orderId();
		this.amount = event.amount();

		// 預約 10 分鐘支付超時任務
		this.paymentDeadlineId = deadlineManager.schedule(Duration.ofMinutes(10), "payment-deadline", event);

		for (OrderItem item : event.items()) {
			commandGateway.send(new ReduceStockCommand(item.productId(), this.orderId, item.quantity()))
					.exceptionally(ex -> {
						synchronized (this) {
							if (!isCancelling) {
								isCancelling = true;
								log.error("[Saga] 資源預留失敗 (產品: {})。發起自動取消機制。", item.productId());
								// 💡 系統權威指令：不檢查版本號以確保補償成功
								commandGateway.send(new CancelOrderCommand(this.orderId, null));
							}
						}
						return null;
					});
		}
	}

	/**
	 * 步驟 2：庫存扣減成功
	 * <p>
	 * 記錄成功扣減的資源，並在符合條件下觸發支付模組。
	 * </p>
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(StockReducedEvent event, CommandGateway commandGateway) {
		log.info("[Saga] 產品 {} 庫存預留成功。", event.productId());
		this.reservedItems.add(new OrderItem(event.productId(), event.quantity(), BigDecimal.ZERO));

		synchronized (this) {
			if (this.paymentId == null && !isCancelling) {
				this.paymentId = UUID.randomUUID().toString();
				SagaLifecycle.associateWith("paymentId", this.paymentId);
				commandGateway.send(new CreatePaymentCommand(this.paymentId, this.orderId, this.amount));
			}
		}
	}

	// ##### 2. 支付與結案 (Payment & Completion) #####

	/**
	 * 步驟 3：支付成功處理
	 * <p>
	 * 關閉超時倒數，並通知 Order 聚合根進入出貨準備階段 (NOTIFIED)。
	 * </p>
	 */
	@SagaEventHandler(associationProperty = "paymentId")
	public void on(PaymentProcessedEvent event, CommandGateway commandGateway, DeadlineManager deadlineManager) {
		log.info("[Saga] 支付成功確認。OrderId: {}", event.orderId());
		this.paymentCompleted = true;

		if (this.paymentDeadlineId != null) {
			deadlineManager.cancelSchedule("payment-deadline", this.paymentDeadlineId);
		}
		commandGateway.send(new NotifyShipmentCommand(event.orderId()));
	}

	/**
	 * 流程終點：通知出貨已發出
	 * <p>
	 * 正向流程結束點，回收 Saga 資源。
	 * </p>
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderNotifiedEvent event) {
		log.info("[Saga] 訂單 {} 流程成功結束，歸檔 Saga。", event.orderId());
//		SagaLifecycle.end();
	}

	// ##### 3. 逆向流程與資源回收 (Inverse Flow & Compensation) #####

	/**
	 * 步驟 4：處理訂單取消事件
	 * <p>
	 * 因應使用者取消或系統失敗，執行物理補償邏輯並結束 Saga。
	 * </p>
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderCancelledEvent event, CommandGateway commandGateway) {
		log.warn("[Saga] 訂單 {} 已取消，啟動資源補償流程...", event.orderId());
		performCompensation(commandGateway);
		SagaLifecycle.end();
	}

	/**
	 * 步驟 5：處理退貨流程事件
	 * <p>
	 * 當訂單狀態轉為 RETURNED 時，啟動逆向物流處理。 本方法連動 {@link Payment} 聚合根執行退款動作。
	 * </p>
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderReturnedEvent event, CommandGateway commandGateway) {
		log.warn("[Saga] 訂單 {} 已進入退貨程序，啟動自動化退款與庫存回撥...", event.orderId());
		performCompensation(commandGateway);
		SagaLifecycle.end();
	}

	/**
	 * 統一資源補償邏輯實作 (DRY)
	 * <p>
	 * 此為私有核心方法，負責執行「庫存加回」與「資金原路退回」的物理指令發送。 確保了取消與退貨在資源回收面上的一致性。
	 * </p>
	 */
	private void performCompensation(CommandGateway commandGateway) {
		this.isCancelling = true;

		// 1. 庫存補償：精確還原
		if (!reservedItems.isEmpty()) {
			for (OrderItem item : reservedItems) {
				log.info("[Saga] 補償：還原產品 {} 之庫存 (數量: {})", item.productId(), item.quantity());
				commandGateway.send(new AddStockCommand(item.productId(), this.orderId, item.quantity()));
			}
		}

		// 2. 金流補償：退款或關閉
		if (this.paymentId != null) {
			if (this.paymentCompleted) {
				log.info("[Saga] 補償：已支付，發送退款指令 (PaymentId: {}, Status: REFUNDED)", this.paymentId);
				commandGateway.send(new RefundPaymentCommand(this.paymentId, this.orderId, this.amount));
			} else {
				log.info("[Saga] 補償：未支付，發送關閉指令 (PaymentId: {}, Status: CANCELLED)", this.paymentId);
				commandGateway.send(new CancelPaymentCommand(this.paymentId, this.orderId));
			}
		}
	}

	// ##### 4. 監控與超時 (Monitoring & Timeout) #####

	/**
	 * 支付超時處理器
	 * <p>
	 * 當用戶超時未付，強制觸發系統級取消，無視當前版本號。
	 * </p>
	 */
	@DeadlineHandler(deadlineName = "payment-deadline")
	public void handlePaymentTimeout(OrderCreatedEvent event, CommandGateway commandGateway) {
		synchronized (this) {
			if (!isCancelling && !paymentCompleted) {
				this.isCancelling = true;
				log.warn("[Saga] 支付超時 (OrderId: {})。系統強制執行取消操作。", event.orderId());
				// 💡 系統強制指令：傳入 null version
				commandGateway.send(new CancelOrderCommand(this.orderId, null));
			}
		}
	}
}