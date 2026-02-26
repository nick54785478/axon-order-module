package com.example.demo.application.domain.order.aggregate;

import java.math.BigDecimal;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

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
 * Order Aggregate Root
 *
 * <p>
 * 此 Aggregate 代表「訂單」的強一致性邊界（Consistency Boundary）。
 * </p>
 *
 * <h3>設計原則</h3>
 * <ul>
 * <li>所有狀態變更必須透過 Command</li>
 * <li>Command Handler 不直接修改狀態</li>
 * <li>必須透過 {@link AggregateLifecycle#apply(Object)} 產生 Event</li>
 * <li>狀態僅能在 {@link EventSourcingHandler} 中更新</li>
 * </ul>
 *
 * <h3>Event Sourcing</h3>
 * <p>
 * 此 Aggregate 採用 Event Sourcing：
 * <ul>
 * <li>實際儲存的是事件（Event Store）</li>
 * <li>重新載入時會 replay events 重建狀態</li>
 * </ul>
 * </p>
 */
@Getter
@Slf4j
@Aggregate(snapshotTriggerDefinition = "orderSnapshotTriggerDefinition")
public class Order {

	/**
	 * Aggregate 唯一識別鍵
	 *
	 * Axon 透過此欄位進行： - Command Routing - Event Stream 分群
	 */
	@AggregateIdentifier
	private String orderId;

	/**
	 * 訂單金額
	 *
	 * 使用 BigDecimal 避免浮點數精度問題 在金流 / Saga / Replay 環境中必須使用精確數值型別。
	 */
	private BigDecimal amount;

	/**
	 * 訂單狀態機
	 *
	 * 狀態只會透過 Event Sourcing Handler 改變。
	 */
	private OrderStatus status;

	/**
	 * Axon 需要的無參數建構子
	 *
	 * 用於： - Event Replay - Snapshot 重建
	 */
	protected Order() {
	}

	// ##### Create Order #####

	/**
	 * 建立訂單（Constructor Command Handler）
	 *
	 * <p>
	 * 建立 Aggregate 時：
	 * <ul>
	 * <li>不能直接設定 orderId</li>
	 * <li>必須透過 apply() 產生 OrderCreatedEvent</li>
	 * </ul>
	 * </p>
	 *
	 * @param command 建立訂單指令
	 */
	@CommandHandler
	public Order(CreateOrderCommand command) {

		log.info("[Order] CreateOrderCommand: {}", command.orderId());

		if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {

			throw new IllegalArgumentException("訂單金額必須大於 0");
		}

		AggregateLifecycle.apply(new OrderCreatedEvent(command.orderId(), command.amount()));
	}

	/**
	 * 訂單建立事件發生後更新狀態
	 */
	@EventSourcingHandler
	public void on(OrderCreatedEvent event) {
		this.orderId = event.orderId();
		this.amount = event.amount();
		this.status = OrderStatus.CREATED;
	}

	// ##### Ship Order #####

	/**
	 * 出貨指令處理
	 *
	 * 業務規則：
	 * <ul>
	 * <li>已出貨不可再次出貨</li>
	 * <li>已取消不可出貨</li>
	 * </ul>
	 */
	@CommandHandler
	public void handle(ShipOrderCommand command) {

		log.info("[Order] ShipOrderCommand: {}", command.orderId());

		if (this.status == OrderStatus.SHIPPED) {
			throw new IllegalStateException("訂單已出貨");
		}

		if (this.status == OrderStatus.CANCELLED) {
			throw new IllegalStateException("訂單已取消，不能出貨");
		}

		AggregateLifecycle.apply(new OrderShippedEvent(command.orderId()));
	}
	
    /**
     * 處理手動確認出貨指令 (由新 API 呼叫)
     */
    @CommandHandler
    public void handle(ConfirmOrderShipmentCommand command) {
        if (this.status != OrderStatus.NOTIFIED) {
            throw new IllegalStateException("訂單尚未通知出貨或狀態錯誤，無法確認出貨");
        }
        AggregateLifecycle.apply(new OrderShippedEvent(command.orderId()));
    }
    


	/**
	 * 出貨事件發生後更新狀態
	 */
	@EventSourcingHandler
	public void on(OrderShippedEvent event) {
		this.status = OrderStatus.SHIPPED;
	}
	
	/**
     * 處理通知出貨指令 (由 Saga 呼叫)
     */
    @CommandHandler
    public void handle(NotifyShipmentCommand command) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有 CREATED 狀態的訂單可以通知出貨");
        }
        AggregateLifecycle.apply(new OrderNotifiedEvent(command.orderId()));
    }


    @EventSourcingHandler
    public void on(OrderNotifiedEvent event) {
        this.status = OrderStatus.NOTIFIED;
    }

	// ###### Cancel Order #####

	/**
	 * 取消訂單指令處理
	 *
	 * <p>
	 * 可能由： - 使用者主動取消 - Saga 補償流程觸發
	 * </p>
	 *
	 * 業務規則：
	 * <ul>
	 * <li>已出貨不可取消</li>
	 * <li>已取消不可再次取消</li>
	 * </ul>
	 */
	@CommandHandler
	public void handle(CancelOrderCommand command) {

		log.info("[Order] CancelOrderCommand: {}", command.orderId());

		if (this.status == OrderStatus.SHIPPED) {
			throw new IllegalStateException("已出貨訂單不可取消");
		}

		if (this.status == OrderStatus.CANCELLED) {
			throw new IllegalStateException("訂單已取消");
		}

		AggregateLifecycle.apply(new OrderCancelledEvent(command.orderId()));
	}
	
	/**
	 * 取消事件發生後更新狀態
	 */
	@EventSourcingHandler
	public void on(OrderCancelledEvent event) {
		this.status = OrderStatus.CANCELLED;
	}
}