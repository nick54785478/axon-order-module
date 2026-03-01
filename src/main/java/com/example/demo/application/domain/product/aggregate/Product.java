package com.example.demo.application.domain.product.aggregate;

import java.math.BigDecimal;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateVersion;
import org.axonframework.spring.stereotype.Aggregate;

import com.example.demo.application.domain.product.command.CreateProductCommand;
import com.example.demo.application.domain.product.command.ReduceStockCommand;
import com.example.demo.application.domain.product.command.UpdateProductCommand;
import com.example.demo.application.domain.product.event.ProductCreatedEvent;
import com.example.demo.application.domain.product.event.ProductUpdatedEvent;
import com.example.demo.application.domain.product.event.StockReducedEvent;
import com.example.demo.application.shared.command.AddStockCommand;
import com.example.demo.application.shared.event.StockAddedEvent;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Product Aggregate - 產品聚合根
 *
 * <p>
 * 職責：管理產品基本資訊與庫存生命週期。本類別是金流與物流操作的強一致性邊界 (Consistency Boundary)。
 * </p>
 *
 * <h3>架構特性：</h3>
 * <ul>
 * <li><b>事件溯源 (Event Sourcing)：</b> 狀態完全由 {@link EventSourcingHandler}
 * 重建，事實持久化於 Axon Server。</li>
 * <li><b>樂觀鎖 (Optimistic Locking)：</b> 透過 {@code @AggregateVersion} 欄位實現，防止多人在
 * UI 同時編輯導致的 Lost Update。</li>
 * <li><b>效能優化 (Snapshotting)：</b> 整合快照觸發器，當事件數量達到閾值時自動產生快照，大幅縮短 Aggregate
 * 載入時間。</li>
 * <li><b>分散式補償：</b> 配合 Saga 流程，提供 {@code AddStockCommand} 作為庫存還原的補償動作。</li>
 * </ul>
 */
@Slf4j
@NoArgsConstructor
@Aggregate(snapshotTriggerDefinition = "productSnapshotTriggerDefinition")
public class Product {

	/**
	 * 產品唯一識別碼
	 */
	@AggregateIdentifier
	private String productId;

	/**
	 * 產品名稱
	 */
	private String name;

	/**
	 * 目前可用庫存數量
	 */
	private Integer stock;

	/**
	 * 產品單價
	 */
	private BigDecimal price;

	/**
	 * 聚合根版本號
	 * <p>
	 * Axon 框架會自動管理此值（對應 Event Store 的 Sequence Number）。 當接收到帶有
	 * {@code @TargetAggregateVersion} 的指令時，框架會自動比對版本以確保操作的安全性。
	 * </p>
	 */
	@AggregateVersion
	private Long version;

	// ##### 1. 初始化階段 (Initialization) #####

	/**
	 * 【Constructor Command Handler】建立產品
	 * <p>
	 * 發布產品建立的初始事實，啟動聚合根生命週期。
	 * </p>
	 *
	 * @param command 包含產品初始資訊與庫存的指令
	 * @throws IllegalArgumentException 若價格小於等於零
	 */
	@CommandHandler
	public Product(CreateProductCommand command) {
		log.info("[Product] 處理 CreateProductCommand: {}", command.productId());

		if (command.price().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("產品價格必須大於零");
		}

		AggregateLifecycle.apply(
				new ProductCreatedEvent(command.productId(), command.name(), command.price(), command.initialStock()));
	}

	// ##### 2. 業務執行階段 (Execution) #####

	/**
	 * 【Command Handler】扣減庫存
	 * <p>
	 * 由 Order Saga 流程觸發。若庫存不足則拋出異常，觸發 Saga 的補償流程。
	 * </p>
	 *
	 * @param command 扣減數量與關聯訂單 ID
	 * @throws IllegalStateException 若庫存不足以支應需求
	 */
	@CommandHandler
	public void handle(ReduceStockCommand command) {
		log.info("[Product] 處理 ReduceStockCommand: {}, 扣減數量: {}", this.productId, command.quantity());

		if (this.stock < command.quantity()) {
			throw new IllegalStateException("庫存不足: " + this.productId);
		}

		AggregateLifecycle.apply(new StockReducedEvent(this.productId, command.orderId(), command.quantity()));
	}

	/**
	 * 【Command Handler】還原庫存 (補償邏輯)
	 * <p>
	 * 當 Saga 偵測到支付超時或交易取消時，調用此指令執行「逆向操作」。
	 * </p>
	 *
	 * @param command 還原數量與關聯訂單 ID
	 */
	@CommandHandler
	public void handle(AddStockCommand command) {
		log.info("[Product] 執行庫存補償，還原數量: {} (OrderId: {})", command.quantity(), command.orderId());

		AggregateLifecycle.apply(new StockAddedEvent(this.productId, command.orderId(), command.quantity()));
	}

	/**
	 * 【Command Handler】更新產品資訊
	 * <p>
	 * 利用樂觀鎖機制確保更新是在正確的版本基礎上進行。
	 * </p>
	 *
	 * @param command 包含目標版本號與新資訊的指令
	 */
	@CommandHandler
	public void handle(UpdateProductCommand command) {
		log.info("[Product] 處理 UpdateProductCommand: {}, 指令版本: {}, 目前版本: {}", this.productId, command.version(),
				this.version);

		if (command.price().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("更新失敗：價格必須大於零");
		}

		AggregateLifecycle.apply(new ProductUpdatedEvent(this.productId, command.name(), command.price()));
	}

	// ##### 3. 狀態重組階段 (Event Sourcing Handlers) #####

	@EventSourcingHandler
	public void on(ProductCreatedEvent event) {
		this.productId = event.productId();
		this.name = event.name();
		this.price = event.price();
		this.stock = event.initialStock();
	}

	@EventSourcingHandler
	public void on(StockReducedEvent event) {
		this.stock -= event.quantity();
	}

	@EventSourcingHandler
	public void on(StockAddedEvent event) {
		this.stock += event.quantity();
	}

	@EventSourcingHandler
	public void on(ProductUpdatedEvent event) {
		this.name = event.name();
		this.price = event.price();
	}
}