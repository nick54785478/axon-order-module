package com.example.demo.application.domain.product.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * ReduceStockCommand - 扣減庫存指令
 * <p>
 * 通常由 Order Saga 流程發起，用於確保訂單所需的商品庫存被保留。
 * </p>
 *
 * @param productId 產品識別碼
 * @param quantity  欲扣減的數量
 */
public record ReduceStockCommand(
	    @TargetAggregateIdentifier 
	    String productId, 
		String orderId, // 確保 Command 帶入 OrderId，以便傳遞給 Event
	    Integer quantity
	) {}