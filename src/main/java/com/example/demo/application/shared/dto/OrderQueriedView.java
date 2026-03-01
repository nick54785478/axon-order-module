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
	 * 訂單品項列表
	 * <p>
	 * 包含此訂單下的所有商品資訊，方便前端一次顯示完整詳情。
	 * </p>
	 */
	private List<OrderItemQueriedView> items;

	/**
	 * OrderItemQueriedView - 用於 API 回傳的品項 DTO (Record) * @param productId 產品識別碼
	 * 
	 * @param quantity 訂購數量
	 * @param price    購買時的單價
	 */
	public record OrderItemQueriedView(String productId, Integer quantity, BigDecimal price) {
	}
}
