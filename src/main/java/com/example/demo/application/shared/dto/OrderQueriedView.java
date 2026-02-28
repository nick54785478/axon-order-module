package com.example.demo.application.shared.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderQueriedView {

	private String orderId;

	private BigDecimal amount;

	private String status;

	/**
	 * * 新增：訂單品項 DTO 列表
	 */
	private List<OrderItemQueriedView> items;

	/**
	 * 用於 API 回傳的品項 DTO
	 */
	public record OrderItemQueriedView(String productId, Integer quantity, BigDecimal price) {
	}
}
