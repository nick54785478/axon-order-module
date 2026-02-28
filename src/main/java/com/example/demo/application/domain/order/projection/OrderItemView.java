package com.example.demo.application.domain.order.projection;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OrderItemView - 訂單品項 (查詢端模型)
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemView {
	
	private String productId;
	
	private Integer quantity;
	
	private BigDecimal price;
}