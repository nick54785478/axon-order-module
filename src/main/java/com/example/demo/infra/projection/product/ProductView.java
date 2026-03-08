package com.example.demo.infra.projection.product;

import java.math.BigDecimal;

import jakarta.persistence.Column;
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
 * 此實體持久化於 MySQL 中，專供前端 API 進行高性能的產品列表檢索與庫存展示。 它是對 {@code Product} 聚合根發布之事件的投影
 * (Projection) 結果。
 * </p>
 *
 * <h3>核心設計：</h3>
 * <ul>
 * <li><b>最終一致性：</b> 此模型可能與 Command 端存在極短的時間差。</li>
 * <li><b>樂觀鎖同步：</b> 存儲 {@code version} 欄位，確保前端拿到的資料版本與 Event Store 同步。</li>
 * <li><b>狀態解耦：</b> 僅包含展示所需的欄位，不包含任何複雜的業務校驗邏輯。</li>
 * </ul>
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
	@Column(length = 64)
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
	 * 樂觀鎖版本號
	 * <p>
	 * 對應 Event Store 中的 Sequence Number。 前端執行更新指令時必須攜帶此值，以進行衝突偵測。
	 * </p>
	 */
	private Long version;

	// ##### 狀態變更方法 (State Mutators) #####

	/**
	 * 初始化產品資訊
	 * <p>
	 * 當接收到 {@code ProductCreatedEvent} 時呼叫。
	 * </p>
	 */
	public void markCreated(String name, BigDecimal price, Integer initialStock, Long version) {
		this.name = name;
		this.price = price;
		this.stock = initialStock;
		this.version = version;
	}

	/**
	 * 更新產品基本資訊
	 * <p>
	 * 當接收到 {@code ProductUpdatedEvent} 時呼叫。
	 * </p>
	 */
	public void updateInfo(String name, BigDecimal price, Long version) {
		this.name = name;
		this.price = price;
		this.version = version;
	}

	/**
	 * 執行庫存扣減
	 * <p>
	 * 當接收到 {@code StockReducedEvent} 時呼叫。
	 * </p>
	 * * @param quantity 扣減數量
	 * 
	 * @param version 新的事件版本號
	 */
	public void reduceStock(Integer quantity, Long version) {
		// 投影端不執行嚴格業務檢查（那是聚合根的責任），僅做基本的防禦
		this.stock = (this.stock != null) ? Math.max(0, this.stock - quantity) : 0;
		this.version = version;
	}

	/**
	 * 執行庫存增加 (補償或進貨)
	 * <p>
	 * 當接收到 {@code StockAddedEvent} 時呼叫。
	 * </p>
	 * * @param quantity 增加數量
	 * 
	 * @param version 新的事件版本號
	 */
	public void addStock(Integer quantity, Long version) {
		this.stock = (this.stock != null) ? this.stock + quantity : quantity;
		this.version = version;
	}

	// ##### 查詢輔助方法 (Business Predicates) #####

	/**
	 * 判斷是否還有庫存
	 */
	public boolean isAvailable() {
		return this.stock != null && this.stock > 0;
	}
}