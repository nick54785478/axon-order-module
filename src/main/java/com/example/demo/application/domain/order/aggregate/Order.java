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
 * Order Aggregate Root - 訂單聚合根
 *
 * <p>
 * 此聚合根代表「訂單」系統的核心業務邏輯與強一致性邊界。 遵循以下 DDD 與 Event Sourcing 設計原則：
 * </p>
 * <ul>
 * <li><b>狀態保護：</b> 所有狀態變更必須透過 Command 觸發業務校驗，禁止外部直接修改。</li>
 * <li><b>職責分離：</b> {@link CommandHandler} 負責執行業務決策；{@link EventSourcingHandler}
 * 負責根據事實更新狀態。</li>
 * <li><b>事件溯源：</b> 透過重播歷史事件（Events Replay）來恢復聚合根的最完整狀態。</li>
 * </ul>
 *
 * @author YourName
 */
@Slf4j
@Getter
@Aggregate(snapshotTriggerDefinition = "orderSnapshotTriggerDefinition")
public class Order {

	/**
	 * 聚合根唯一識別碼 (OrderId)
	 * <p>
	 * Axon 透過此識別碼進行 Command Routing 與 Event Stream 的分群處理。
	 * </p>
	 */
	@AggregateIdentifier
	private String orderId;

	/**
	 * 訂單內部實體：品項清單 (Order Items)
	 * <p>
	 * 在聚合根內部作為狀態保存，確保訂單明細的一致性。
	 * </p>
	 */
	private List<OrderItem> items;

	/**
	 * 訂單總金額
	 * <p>
	 * 使用 {@link BigDecimal} 確保金流計算的精確度，防止浮點數誤差。
	 * </p>
	 */
	private BigDecimal amount;

	/**
	 * 訂單生命週期狀態
	 * <p>
	 * 狀態的改變嚴格受限於 {@link EventSourcingHandler}。
	 * </p>
	 */
	private OrderStatus status;

	/**
	 * Axon 框架所需的無參數建構子
	 * <p>
	 * 用於 Event Replay 或從快照 (Snapshot) 重建聚合根實例時調用。
	 * </p>
	 */
	protected Order() {
	}

	// ##### 1. 建立訂單 (Constructor Command Handler) #####

	/**
	 * 處理建立訂單指令 *
	 * <p>
	 * 這是聚合根的生命週期起點。在此執行關鍵的「進場校驗」：
	 * </p>
	 * <ol>
	 * <li>驗證訂單是否包含品項。</li>
	 * <li>計算並校對總金額，確保金額與品項明細一致。</li>
	 * </ol>
	 *
	 * @param command 建立訂單指令，包含品項、金額與 ID
	 * @throws IllegalArgumentException 當校驗失敗時拋出，阻止訂單建立
	 */
	@CommandHandler
	public Order(CreateOrderCommand command) {
		log.info("[Order] 接收到 CreateOrderCommand: {}", command.orderId());

		// 業務校驗：訂單必須包含品項
		if (command.items() == null || command.items().isEmpty()) {
			throw new IllegalArgumentException("訂單必須包含至少一個品項");
		}

		// 業務校驗：重新計算總金額以防前端傳入錯誤數值 (防腐處理)
		BigDecimal calculatedAmount = command.items().stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		if (command.amount() != null && command.amount().compareTo(calculatedAmount) != 0) {
			log.warn("[Order] 傳入金額 {} 與計算金額 {} 不符，以計算結果為主", command.amount(), calculatedAmount);
		}

		log.info("[Order] 業務規則校驗通過，發布 OrderCreatedEvent");

		// 產生事實：將決策結果轉化為事件發布
		AggregateLifecycle.apply(new OrderCreatedEvent(command.orderId(), command.items(), calculatedAmount));
	}

	/**
	 * 響應訂單建立事件
	 * <p>
	 * 此方法僅負責還原內存狀態，不含任何業務邏輯。
	 * </p>
	 */
	@EventSourcingHandler
	public void on(OrderCreatedEvent event) {
		this.orderId = event.orderId();
		// 為了確保聚合根內部狀態的不可變性 (Immutability)，存入新的 ArrayList 複本
		this.items = new ArrayList<>(event.items());
		this.amount = event.amount();
		this.status = OrderStatus.CREATED;
	}

	// ##### 2. 出貨流程 (Shipment Handling) #####

	/**
	 * 處理出貨指令
	 * <p>
	 * 校驗目前狀態是否允許出貨（防禦性編程）。
	 * </p>
	 */
	@CommandHandler
	public void handle(ShipOrderCommand command) {
		log.info("[Order] 處理 ShipOrderCommand: {}", command.orderId());

		if (this.status == OrderStatus.SHIPPED) {
			throw new IllegalStateException("訂單已出貨，不可重複執行");
		}
		if (this.status == OrderStatus.CANCELLED) {
			throw new IllegalStateException("訂單已取消，無法執行出貨");
		}

		AggregateLifecycle.apply(new OrderShippedEvent(command.orderId()));
	}

	/**
	 * 處理手動確認出貨指令 (由外部管理 API 呼叫)
	 * <p>
	 * 通常用於處理 Saga 流程之外的特殊人工介入場景。
	 * </p>
	 */
	@CommandHandler
	public void handle(ConfirmOrderShipmentCommand command) {
		if (this.status != OrderStatus.NOTIFIED) {
			throw new IllegalStateException("訂單尚未通知出貨 (狀態不符)，無法確認手動出貨");
		}
		AggregateLifecycle.apply(new OrderShippedEvent(command.orderId()));
	}

	/**
	 * 響應出貨事件
	 */
	@EventSourcingHandler
	public void on(OrderShippedEvent event) {
		this.status = OrderStatus.SHIPPED;
	}

	// ##### 3. 流程推進 (Notification Handling) #####

	/**
	 * 處理通知出貨指令 (通常由 Saga 於支付成功後呼叫)
	 * <p>
	 * 代表支付已完成，準備將訂單轉向出貨部門。
	 * </p>
	 */
	@CommandHandler
	public void handle(NotifyShipmentCommand command) {
		if (this.status != OrderStatus.CREATED) {
			throw new IllegalStateException("目前訂單狀態為 " + this.status + "，不允許執行通知出貨");
		}
		AggregateLifecycle.apply(new OrderNotifiedEvent(command.orderId()));
	}

	/**
	 * 響應通知出貨事件
	 */
	@EventSourcingHandler
	public void on(OrderNotifiedEvent event) {
		this.status = OrderStatus.NOTIFIED;
	}

	// ##### 4. 取消流程 (Cancellation Handling) #####

	/**
	 * 處理取消訂單指令
	 * <p>
	 * 可能來自使用者主動操作，或 Saga 超時補償流程。
	 * </p>
	 */
	@CommandHandler
	public void handle(CancelOrderCommand command) {
		log.info("[Order] 處理 CancelOrderCommand: {}", command.orderId());

		if (this.status == OrderStatus.SHIPPED) {
			throw new IllegalStateException("已出貨訂單具備法律效力或物流成本，不可隨意取消");
		}
		if (this.status == OrderStatus.CANCELLED) {
			log.warn("[Order] 訂單 {} 已經是取消狀態，忽略重複的取消指令。", command.orderId());
			return;
		}

		AggregateLifecycle.apply(new OrderCancelledEvent(command.orderId()));
	}

	/**
	 * 響應取消事件
	 */
	@EventSourcingHandler
	public void on(OrderCancelledEvent event) {
		this.status = OrderStatus.CANCELLED;
	}
}