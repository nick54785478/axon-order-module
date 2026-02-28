package com.example.demo.application.domain.order.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.example.demo.application.domain.order.aggregate.vo.OrderItem;

/**
 * CreateOrderCommand - 建立訂單指令
 * <p>
 * 此指令由 API 觸發，代表使用者的下單意圖。
 * </p>
 * 
 * @param orderId 訂單唯一識別碼，使用 {@link TargetAggregateIdentifier} 告知 Axon 路由至哪一個
 *                Aggregate。
 * @param items   訂單品項清單。
 * @param amount  預期總金額 (Aggregate 內部會再次校驗此金額與品項之和是否一致)。
 */
public record CreateOrderCommand(@TargetAggregateIdentifier String orderId, List<OrderItem> items, BigDecimal amount) {

	public CreateOrderCommand {
		if (orderId == null) {
			orderId = UUID.randomUUID().toString();
		}
	}
}