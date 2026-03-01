package com.example.demo.application.domain.product.command;

import java.math.BigDecimal;

import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.modelling.command.TargetAggregateVersion;

/**
 * UpdateProductCommand - 更新產品指令
 * 
 * @param productId 產品 ID
 * @param version   預期版本號 (用於樂觀鎖校驗)
 * @param name      新的產品名稱
 * @param price     新的產品單價
 */
public record UpdateProductCommand(@TargetAggregateIdentifier String productId, @TargetAggregateVersion Long version,
		String name, BigDecimal price) {
}