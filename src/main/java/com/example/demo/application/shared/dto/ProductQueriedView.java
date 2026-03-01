package com.example.demo.application.shared.dto;

import java.math.BigDecimal;

/**
 * ProductQueriedView - 產品查詢結果 DTO *
 * <p>
 * 此為不可變的 Record，專門用於 API 回傳。 包含前端展示所需的核心欄位，並提供基礎的業務狀態判斷。
 * </p>
 */
public record ProductQueriedView(String productId, String name, BigDecimal price, Integer stock) {
	/**
	 * 輔助屬性：判斷目前是否有庫存
	 * <p>
	 * 前端可根據此欄位決定「加入購物車」按鈕的可點擊狀態。
	 * </p>
	 */
	public boolean isAvailable() {
		return stock != null && stock > 0;
	}
}