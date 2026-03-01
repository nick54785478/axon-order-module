package com.example.demo.application.domain.order.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.modelling.command.TargetAggregateVersion;

/**
 * CancelOrderCommand - 取消訂單指令
 * 
 * @param orderId 訂單 ID
 * @param version 樂觀鎖版本號 (傳入 null 代表系統強制執行，不檢查版本)
 */
public record CancelOrderCommand(@TargetAggregateIdentifier String orderId, @TargetAggregateVersion Long version) {
	// 增加一個便捷建構子，專供 Saga 或內部系統使用
	public CancelOrderCommand(String orderId) {
		this(orderId, null);
	}
}