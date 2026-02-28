package com.example.demo.application.domain.order.aggregate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import com.example.demo.application.domain.order.aggregate.vo.OrderItem;
import com.example.demo.application.domain.order.aggregate.vo.OrderStatus;
import com.example.demo.application.domain.order.command.CancelOrderCommand;
import com.example.demo.application.domain.order.command.ConfirmOrderShipmentCommand;
import com.example.demo.application.domain.order.command.CreateOrderCommand;
import com.example.demo.application.domain.order.command.NotifyShipmentCommand;
import com.example.demo.application.domain.order.command.ShipOrderCommand;
import com.example.demo.application.domain.order.event.OrderCancelledEvent;
import com.example.demo.application.domain.order.event.OrderCreatedEvent;
import com.example.demo.application.domain.order.event.OrderNotifiedEvent;
import com.example.demo.application.domain.order.event.OrderShippedEvent;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Order Aggregate Root - 訂單聚合根 *
 * <p>
 * 負責守護訂單與品項的一致性。確保： 1. 訂單必須包含至少一個品項。 2. 訂單總金額與品項小計之和必須一致。
 * </p>
 */
@Getter
@Slf4j
@Aggregate(snapshotTriggerDefinition = "orderSnapshotTriggerDefinition")
public class Order {

	@AggregateIdentifier
	private String orderId;

	/**
	 * 內部實體：訂單品項清單
	 */
	private List<OrderItem> items;

	/**
	 * 訂單總金額
	 */
	private BigDecimal amount;

	/**
	 * 訂單狀態
	 */
	private OrderStatus status;

	protected Order() {
		// Axon 需要的無參數建構子
	}

	/**
	 * 【Constructor Command Handler】建立訂單 * @param command 包含品項清單的指令
	 */
	@CommandHandler
	public Order(CreateOrderCommand command) {
		log.info("[Order] 處理 CreateOrderCommand: {}", command.orderId());

		// 1. 業務校驗：訂單必須包含品項
		if (command.items() == null || command.items().isEmpty()) {
			throw new IllegalArgumentException("訂單必須包含至少一個品項");
		}

		// 2. 業務校驗：計算總金額並校對 (或直接以計算結果為主)
		BigDecimal calculatedAmount = command.items().stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		log.info("[Order] 計算總金額: {}", calculatedAmount);

		// 3. 發布事件 (包含品項明細與計算後的金額)
		AggregateLifecycle.apply(new OrderCreatedEvent(command.orderId(), command.items(), calculatedAmount));
	}

	/**
	 * 【Event Sourcing Handler】訂單建立後恢復狀態
	 */
	@EventSourcingHandler
	public void on(OrderCreatedEvent event) {
		this.orderId = event.orderId();
		// 為了確保不可變性，我們存入一份新的 ArrayList
		this.items = new ArrayList<>(event.items());
		this.amount = event.amount();
		this.status = OrderStatus.CREATED;
	}

	/**
	 * 【Command Handler】出貨處理
	 */
	@CommandHandler
	public void handle(ShipOrderCommand command) {
		if (this.status == OrderStatus.SHIPPED)
			throw new IllegalStateException("訂單已出貨");
		if (this.status == OrderStatus.CANCELLED)
			throw new IllegalStateException("訂單已取消");

		AggregateLifecycle.apply(new OrderShippedEvent(command.orderId()));
	}

	/**
	 * 【Command Handler】手動確認出貨 (由 API 呼叫)
	 */
	@CommandHandler
	public void handle(ConfirmOrderShipmentCommand command) {
		if (this.status != OrderStatus.NOTIFIED) {
			throw new IllegalStateException("訂單尚未通知出貨或狀態錯誤");
		}
		AggregateLifecycle.apply(new OrderShippedEvent(command.orderId()));
	}

	/**
	 * 【Command Handler】通知出貨 (由 Saga 呼叫)
	 */
	@CommandHandler
	public void handle(NotifyShipmentCommand command) {
		if (this.status != OrderStatus.CREATED) {
			throw new IllegalStateException("只有 CREATED 狀態可以通知出貨");
		}
		AggregateLifecycle.apply(new OrderNotifiedEvent(command.orderId()));
	}

	/**
	 * 【Command Handler】取消訂單
	 */
	@CommandHandler
	public void handle(CancelOrderCommand command) {
		if (this.status == OrderStatus.SHIPPED)
			throw new IllegalStateException("已出貨訂單不可取消");
		if (this.status == OrderStatus.CANCELLED)
			throw new IllegalStateException("訂單已取消");

		AggregateLifecycle.apply(new OrderCancelledEvent(command.orderId()));
	}

	// ##### Event Sourcing Handlers (狀態更新) #####

	@EventSourcingHandler
	public void on(OrderShippedEvent event) {
		this.status = OrderStatus.SHIPPED;
	}

	@EventSourcingHandler
	public void on(OrderNotifiedEvent event) {
		this.status = OrderStatus.NOTIFIED;
	}

	@EventSourcingHandler
	public void on(OrderCancelledEvent event) {
		this.status = OrderStatus.CANCELLED;
	}
}