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
 * Product Aggregate - 產品聚合根 *
 * <p>
 * 職責：管理產品資訊與庫存數量。 狀態完全由事件重播還原，事實儲存在 Axon Server 的 Event Store 中。
 * </p>
 */
@Slf4j
@NoArgsConstructor
@Aggregate(snapshotTriggerDefinition = "productSnapshotTriggerDefinition")
public class Product {

	@AggregateIdentifier
	private String productId;
	private String name;
	private Integer stock;
	private BigDecimal price;
	@AggregateVersion
	private Long version; // Axon 會自動管理此值，並將其與 Command 中的版本號比對。

	/**
	 * 【指令處理器】建立產品
	 */
	@CommandHandler
	public Product(CreateProductCommand command) {
		log.info("[Product] 處理 CreateProductCommand: {}", command.productId());

		// 業務校驗
		if (command.price().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("產品價格必須大於零");
		}

		// 發布事實
		AggregateLifecycle.apply(
				new ProductCreatedEvent(command.productId(), command.name(), command.price(), command.initialStock()));
	}

	/**
	 * 【指令處理器】扣減庫存
	 */
	@CommandHandler
	public void handle(ReduceStockCommand command) {
		if (this.stock < command.quantity()) {
			throw new IllegalStateException("庫存不足: " + this.productId);
		}

		// 發布包含 orderId 的事件，讓 Saga 知道這是哪筆訂單扣的
		AggregateLifecycle.apply(new StockReducedEvent(this.productId, command.orderId(), command.quantity()));
	}

	/**
	 * 【指令處理器】還原庫存 (補償邏輯)
	 * <p>
	 * 當 Saga 發現支付超時或訂單取消時，會發送此指令。
	 * </p>
	 */
	@CommandHandler
	public void handle(AddStockCommand command) {
		log.info("[Product] 執行庫存補償，還原數量: {} (OrderId: {})", command.quantity(), command.orderId());

		AggregateLifecycle.apply(new StockAddedEvent(this.productId, command.orderId(), command.quantity()));
	}

	/**
	 * 【指令處理器】更新產品資訊
	 * <p>
	 * Axon 會自動比對指令中的 version 與目前的 Aggregate 版本。
	 * </p>
	 */
	@CommandHandler
	public void handle(UpdateProductCommand command) {
		log.info("[Product] 處理 UpdateProductCommand: {}, Version: {}", command.productId(), command.version());

		// 業務規則校驗
		if (command.price().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("更新失敗：價格必須大於零");
		}

		// 發布更新事件
		AggregateLifecycle.apply(new ProductUpdatedEvent(this.productId, command.name(), command.price()));
	}

	// ##### Event Sourcing Handlers (重建狀態) #####

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
		// 還原庫存，狀態重回正確數值
		this.stock += event.quantity();
	}

	/**
	 * 響應更新事件：更新內部狀態
	 */
	@EventSourcingHandler
	public void on(ProductUpdatedEvent event) {
		this.name = event.name();
		this.price = event.price();
	}
}