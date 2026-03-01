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
 * <p>
 * 整合了 isCancelling 旗標與 synchronized 鎖，確保在多品項扣減失敗或併發超時情況下， 僅會發送一次取消指令，維護系統的冪等性。
 * </p>
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
	 * 關鍵旗標：防止重複觸發取消流程
	 */
	private boolean isCancelling = false;

	/**
	 * 追蹤真正成功保留的品項，確保補償精確度
	 */
	private List<OrderItem> reservedItems = new ArrayList<>();

	/**
	 * 步驟 1：訂單建立 -> 發起庫存扣減
	 */
	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderCreatedEvent event, CommandGateway commandGateway, DeadlineManager deadlineManager) {
		log.info("[Saga] 訂單 {} 已建立。開始請求扣減庫存。", event.orderId());

		this.orderId = event.orderId();
		this.amount = event.amount();

		// 預約支付超時任務
		this.paymentDeadlineId = deadlineManager.schedule(Duration.ofMinutes(10), "payment-deadline", event);

		// 遍歷品項發送指令
		for (OrderItem item : event.items()) {
			commandGateway.send(new ReduceStockCommand(item.productId(), this.orderId, item.quantity()))
					.exceptionally(ex -> {
						// 使用 synchronized 確保檢查與標記 flag 是原子操作
						synchronized (this) {
							if (!isCancelling) {
								isCancelling = true;
								log.error("[Saga] 產品 {} 扣減失敗 (原因: {})。發起唯一一次取消指令。", item.productId(), ex.getMessage());
								commandGateway.send(new CancelOrderCommand(this.orderId));
							}
						}
						return null;
					});
		}
	}

	/**
	 * 步驟 2：庫存扣減成功
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(StockReducedEvent event, CommandGateway commandGateway) {
		log.info("[Saga] 產品 {} 庫存保留成功。", event.productId());

		// 即使正在取消中，若收到成功的事件也要記錄，以便後續 Cancel 事件能將其還原
		this.reservedItems.add(new OrderItem(event.productId(), event.quantity(), BigDecimal.ZERO));

		// 如果尚未啟動支付且「非取消中」，則啟動支付流程
		synchronized (this) {
			if (this.paymentId == null && !isCancelling) {
				this.paymentId = UUID.randomUUID().toString();
				SagaLifecycle.associateWith("paymentId", this.paymentId);
				commandGateway.send(new CreatePaymentCommand(this.paymentId, this.orderId, this.amount));
			}
		}
	}

	/**
	 * 步驟 3：支付完成
	 */
	@SagaEventHandler(associationProperty = "paymentId")
	public void on(PaymentProcessedEvent event, CommandGateway commandGateway, DeadlineManager deadlineManager) {
		log.info("[Saga] 支付成功！OrderId: {}", event.orderId());
		this.paymentCompleted = true;

		if (this.paymentDeadlineId != null) {
			deadlineManager.cancelSchedule("payment-deadline", this.paymentDeadlineId);
		}
		commandGateway.send(new NotifyShipmentCommand(event.orderId()));
	}

	/**
	 * 步驟 4：補償機制 (由 OrderCancelledEvent 觸發)
	 */
	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderCancelledEvent event, CommandGateway commandGateway) {
		log.warn("[Saga] 偵測到訂單 {} 已取消，執行資源回收...", event.orderId());

		// 確保 isCancelling 為 true，防止後續其他異常再度觸發
		this.isCancelling = true;

		// 1. 精確還原已扣除的庫存
		if (!reservedItems.isEmpty()) {
			for (OrderItem item : reservedItems) {
				log.info("[Saga] 補償：還原產品 {} 庫存", item.productId());
				commandGateway.send(new AddStockCommand(item.productId(), this.orderId, item.quantity()));
			}
		}

		// 2. 處理支付狀態 (退款或關閉)
		if (this.paymentId != null) {
			if (this.paymentCompleted) {
				commandGateway.send(new RefundPaymentCommand(this.paymentId, this.orderId, this.amount));
			} else {
				commandGateway.send(new CancelPaymentCommand(this.paymentId, this.orderId));
			}
		}

		SagaLifecycle.end();
	}

	/**
	 * 支付超時處理 (Deadline)
	 */
	@DeadlineHandler(deadlineName = "payment-deadline")
	public void handlePaymentTimeout(OrderCreatedEvent event, CommandGateway commandGateway) {
		synchronized (this) {
			if (!isCancelling && !paymentCompleted) {
				this.isCancelling = true;
				log.warn("[Saga] 支付超時！發起自動取消。OrderId: {}", event.orderId());
				commandGateway.send(new CancelOrderCommand(this.orderId));
			}
		}
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void on(OrderNotifiedEvent event) {
		log.info("[Saga] 訂單流程圓滿結束。");
		SagaLifecycle.end();
	}
}