package com.example.demo.application.domain.product.command;

import java.math.BigDecimal;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * CreateProductCommand - 建立產品指令
 * <p>
 * 此指令由管理後台觸發，代表新增產品的業務意圖。
 * </p>
 *
 * @param productId    產品唯一識別碼 (若為 null 則自動產生 UUID)
 * @param name         產品名稱
 * @param price        產品單價
 * @param initialStock 初始庫存數量
 */
public record CreateProductCommand(@TargetAggregateIdentifier String productId, String name, BigDecimal price,
		Integer initialStock) {
	/**
	 * Compact Constructor: 確保 productId 不為空
	 */
	public CreateProductCommand {
		if (productId == null) {
			productId = java.util.UUID.randomUUID().toString();
		}
	}
}