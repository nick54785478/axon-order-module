package com.example.demo.application.domain.product.projection;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProductView - 產品查詢模型 (Read Model)
 *
 * <p>
 * 此實體持久化於 MySQL 中，專供前端 API 進行高性能的產品列表檢索與庫存展示。
 * </p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "product_view")
public class ProductView {

	/**
	 * 產品唯一識別碼 (對應 Aggregate Identifier)
	 */
	@Id
	private String productId;

	/**
	 * 產品名稱
	 */
	private String name;

	/**
	 * 產品單價
	 */
	private BigDecimal price;

	/**
	 * 目前可用庫存數量
	 */
	private Integer stock;

	/**
	 * 核心新增：儲存來自 Aggregate 的版本號
	 * <p>
	 * 此版本號對應 Event Store 中的 Sequence Number。
	 * </p>
	 */
	private Long version;

	// ##### 狀態變更方法 (State Mutators) #####

	/**
	 * 初始化產品資訊
	 * <p>
	 * 當接收到 ProductCreatedEvent 時呼叫。
	 * </p>
	 */
	public void markCreated(String name, BigDecimal price, Integer initialStock, Long version) {
        this.name = name;
        this.price = price;
        this.stock = initialStock;
        this.version = version;
    }
	
	public void updateInfo(String name, BigDecimal price, Long version) {
        this.name = name;
        this.price = price;
        this.version = version;
    }
	
	public void updateStock(Integer newStock, Long version) {
        this.stock = newStock;
        this.version = version;
    }

	/**
	 * 執行庫存扣減
	 * <p>
	 * 當接收到 StockReducedEvent 時呼叫，確保讀取端的庫存同步減少。
	 * </p>
	 * 
	 * @param quantity 扣減數量
	 */
	public void reduceStock(Integer quantity) {
		if (this.stock >= quantity) {
			this.stock -= quantity;
		} else {
			// 在查詢端這通常代表事件順序異常，可記錄日誌
			this.stock = 0;
		}
	}

	// ##### 查詢輔助方法 (Business Predicates) #####

	/**
	 * 判斷是否還有庫存
	 */
	public boolean isAvailable() {
		return this.stock != null && this.stock > 0;
	}
}