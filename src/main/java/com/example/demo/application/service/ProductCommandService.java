package com.example.demo.application.service;

import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.product.command.CreateProductCommand;
import com.example.demo.application.domain.product.command.ReduceStockCommand;
import com.example.demo.application.shared.command.AddStockCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProductCommandService - 產品指令應用服務
 *
 * <p>
 * 本服務屬於 Application Layer，負責將外部請求（API 或 Saga）封裝為指令並派發。 配合系統的 Event Sourcing
 * 架構，所有庫存變動皆透過此處發送至 Product 聚合根執行。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandService {

	/**
	 * Axon 提供的指令閘道器，負責將指令路由至正確的聚合根實例
	 */
	private final CommandGateway commandGateway;

	/**
	 * 執行建立產品指令
	 *
	 * @param command 包含名稱、價格與初始庫存的建立指令
	 * @return CompletableFuture&lt;String&gt; 成功建立後回傳產生的 productId
	 */
	public CompletableFuture<String> createProduct(CreateProductCommand command) {
		log.info("[Product Service] 接收到產品建立請求: {}", command.name());
		return commandGateway.send(command);
	}

	/**
	 * 執行庫存扣減指令
	 * <p>
	 * 注意：在新的版本中，必須提供 orderId 以便 Saga 進行事務追蹤與後續可能的補償流程。
	 * </p>
	 *
	 * @param productId 產品識別碼
	 * @param orderId   關聯的訂單 ID (若為手動調整可傳入 "MANUAL")
	 * @param quantity  欲扣減的數量
	 * @return CompletableFuture&lt;Void&gt;
	 */
	public CompletableFuture<Void> reduceStock(String productId, String orderId, Integer quantity) {
		log.info("[Product Service] 請求扣減庫存: Product={}, Order={}, Quantity={}", productId, orderId, quantity);
		// 修正：調用包含 orderId 的建構子
		return commandGateway.send(new ReduceStockCommand(productId, orderId, quantity));
	}

	/**
	 * 執行庫存增加/還原指令
	 * <p>
	 * 通常用於補償流程 (Compensation Logic) 或進貨操作。
	 * </p>
	 *
	 * @param productId 產品識別碼
	 * @param orderId   關聯的訂單 ID (用於追蹤是哪筆訂單退回的庫存)
	 * @param quantity  欲增加的數量
	 * @return CompletableFuture&lt;Void&gt;
	 */
	public CompletableFuture<Void> addStock(String productId, String orderId, Integer quantity) {
		log.info("[Product Service] 請求還原庫存: Product={}, Order={}, Quantity={}", productId, orderId, quantity);
		// 發送補償指令至 Product Aggregate
		return commandGateway.send(new AddStockCommand(productId, orderId, quantity));
	}
}