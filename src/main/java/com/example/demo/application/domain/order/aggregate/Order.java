package com.example.demo.application.domain.order.aggregate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateVersion;
import org.axonframework.spring.stereotype.Aggregate;

import com.example.demo.application.domain.order.aggregate.vo.OrderItem;
import com.example.demo.application.domain.order.aggregate.vo.OrderStatus;
import com.example.demo.application.domain.order.command.CancelOrderCommand;
import com.example.demo.application.domain.order.command.ConfirmOrderShipmentCommand;
import com.example.demo.application.domain.order.command.CreateOrderCommand;
import com.example.demo.application.domain.order.command.NotifyShipmentCommand;
import com.example.demo.application.domain.order.command.ReturnOrderCommand;
import com.example.demo.application.domain.order.command.ShipOrderCommand;
import com.example.demo.application.domain.order.event.OrderCancelledEvent;
import com.example.demo.application.domain.order.event.OrderCreatedEvent;
import com.example.demo.application.domain.order.event.OrderNotifiedEvent;
import com.example.demo.application.domain.order.event.OrderReturnedEvent;
import com.example.demo.application.domain.order.event.OrderShippedEvent;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Order Aggregate Root - 訂單聚合根
 *
 * <p>
 * 此聚合根代表「訂單」系統的核心業務邏輯與強一致性邊界。 整合了樂觀鎖機制與精確的狀態機控制。
 * </p>
 */
@Slf4j
@Getter
@Aggregate(snapshotTriggerDefinition = "orderSnapshotTriggerDefinition")
public class Order {

	@AggregateIdentifier
	private String orderId;

	@AggregateVersion
	private Long version;

	private List<OrderItem> items;
	private BigDecimal amount;
	private OrderStatus status;

	protected Order() {
		// Required by Axon for Event Replay
	}

	// ##### 1. 建立階段 (Initialization) #####

	@CommandHandler
	public Order(CreateOrderCommand command) {
		log.info("[Order] 接收到 CreateOrderCommand: {}", command.orderId());

		if (command.items() == null || command.items().isEmpty()) {
			throw new IllegalArgumentException("訂單必須包含至少一個品項");
		}

		BigDecimal calculatedAmount = command.items().stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		AggregateLifecycle.apply(new OrderCreatedEvent(command.orderId(), command.items(), calculatedAmount));
	}

	@EventSourcingHandler
	public void on(OrderCreatedEvent event) {
		this.orderId = event.orderId();
		this.items = new ArrayList<>(event.items());
		this.amount = event.amount();
		this.status = OrderStatus.CREATED;
	}

	// ##### 2. 出貨與推進流程 (Execution & Transition) #####

	/**
	 * 處理手動確認出貨指令
	 * <p>
	 * 此方法對應 API {@code /orders/{orderId}/ship}。 業務規則：僅能在「已通知出貨
	 * (NOTIFIED)」狀態下手動確認出貨。
	 * </p>
	 */
	@CommandHandler
	public void handle(ConfirmOrderShipmentCommand command) {
		log.info("[Order] 處理手動確認出貨指令: {}, Version: {}", command.orderId(), command.version());

		// 1. 冪等防護
		if (this.status == OrderStatus.SHIPPED) {
			log.warn("[Order] 訂單 {} 已是出貨狀態，略過處理。", this.orderId);
			return;
		}

		// 2. 狀態防禦：必須是支付成功後轉跳的 NOTIFIED 狀態才能出貨
		if (this.status != OrderStatus.NOTIFIED) {
			throw new IllegalStateException("無法手動出貨：目前狀態為 " + this.status + "，必須先完成支付通知");
		}

		AggregateLifecycle.apply(new OrderShippedEvent(command.orderId()));
	}

	/**
	 * 處理系統/自動出貨指令 (可選)
	 */
	@CommandHandler
	public void handle(ShipOrderCommand command) {
		if (this.status == OrderStatus.SHIPPED)
			return;
		if (this.status == OrderStatus.CANCELLED) {
			throw new IllegalStateException("訂單已取消，無法執行出貨");
		}
		AggregateLifecycle.apply(new OrderShippedEvent(command.orderId()));
	}

	/**
	 * 處理通知出貨指令 (通常由 Saga 於支付成功後呼叫)
	 */
	@CommandHandler
	public void handle(NotifyShipmentCommand command) {
		if (this.status != OrderStatus.CREATED) {
			throw new IllegalStateException("目前狀態為 " + this.status + "，不允許通知出貨");
		}
		AggregateLifecycle.apply(new OrderNotifiedEvent(command.orderId()));
	}

	@EventSourcingHandler
	public void on(OrderShippedEvent event) {
		this.status = OrderStatus.SHIPPED;
	}

	@EventSourcingHandler
	public void on(OrderNotifiedEvent event) {
		this.status = OrderStatus.NOTIFIED;
	}

	// ##### 3. 取消與退貨流程 (Cancellation & Returns) #####

	@CommandHandler
	public void handle(CancelOrderCommand command) {
		log.info("[Order] 處理 CancelOrderCommand: {}, Version: {}", command.orderId(), command.version());

		if (this.status == OrderStatus.CANCELLED)
			return;

		if (this.status == OrderStatus.SHIPPED) {
			throw new IllegalStateException("已出貨訂單不可取消，請改用退貨流程");
		}

		AggregateLifecycle.apply(new OrderCancelledEvent(command.orderId()));
	}

	@CommandHandler
	public void handle(ReturnOrderCommand command) {
		log.info("[Order] 處理退貨請求: {}, 原因: {}", command.orderId(), command.reason());

		if (this.status == OrderStatus.RETURNED)
			return;

		if (this.status != OrderStatus.SHIPPED) {
			throw new IllegalStateException("無法退貨：目前狀態為 " + this.status + "，僅已出貨訂單可申請退貨");
		}

		AggregateLifecycle.apply(new OrderReturnedEvent(this.orderId, new ArrayList<>(this.items), command.reason()));
	}

	@EventSourcingHandler
	public void on(OrderCancelledEvent event) {
		this.status = OrderStatus.CANCELLED;
	}

	@EventSourcingHandler
	public void on(OrderReturnedEvent event) {
		this.status = OrderStatus.RETURNED;
	}
}