package com.example.demo.application.domain.order.projection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OrderView - 訂單查詢模型 (Read Model)
 *
 * <p>
 * 此實體屬於 Infrastructure Layer，是訂單資訊在資料庫中的持久化表示。 專為「查詢優化」設計，透過監聽領域事件（Domain
 * Events）來同步最新的訂單狀態。
 * </p>
 *
 * <h3>設計特色：</h3>
 * <ul>
 * <li><b>一對多映射：</b> 使用 JPA 的 {@code @ElementCollection}
 * 處理訂單品項，簡化了資料結構的複雜度。</li>
 * <li><b>充血模型應用：</b> 將狀態變更邏輯 (markXXX) 與業務狀態判斷 (canXXX) 封裝在實體中，避免邏輯散落在各個
 * Handler。</li>
 * <li><b>即時查詢：</b> 透過預先計算與聚合（如總金額與狀態），提供 API 毫秒級的響應速度。</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "order_view")
public class OrderView {

	/** 訂單唯一識別碼 (對應 Aggregate 的 OrderId) */
	@Id
	private String orderId;

	/**
	 * 訂單品項清單
	 * <p>
	 * 使用 ElementCollection 存儲簡單的品項數值集合。
	 * </p>
	 * <p>
	 * 設定 FetchType.EAGER 以確保查詢訂單時能一併取得所有品項明細。
	 * </p>
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "order_view_items", joinColumns = @JoinColumn(name = "order_id"))
	private List<OrderItemView> items = new ArrayList<>();

	/** 訂單最終計算總金額 */
	private BigDecimal amount;

	/**
	 * * 訂單目前狀態
	 * <p>
	 * 狀態流轉：CREATED -> NOTIFIED -> SHIPPED (或 CREATED -> CANCELLED)
	 * </p>
	 */
	private String status;

	// ##### 1. 狀態變更方法 (State Mutators - 充血模型) #####

	/**
	 * 標記訂單為已出貨 (SHIPPED)
	 * <p>
	 * 對應事件：OrderShippedEvent
	 * </p>
	 */
	public void markShipped() {
		this.status = "SHIPPED";
	}

	/**
	 * 標記訂單為已通知出貨 (NOTIFIED)
	 * <p>
	 * 代表支付成功，已正式進入待出貨階段。對應事件：OrderNotifiedEvent
	 * </p>
	 */
	public void markNotified() {
		this.status = "NOTIFIED";
	}

	/**
	 * 標記訂單為已取消 (CANCELLED)
	 * <p>
	 * 對應事件：OrderCancelledEvent
	 * </p>
	 */
	public void markCancelled() {
		this.status = "CANCELLED";
	}

	// ##### 2. 業務判斷輔助方法 (Business Predicates) #####

	/**
	 * 判斷目前訂單是否可以執行出貨動作
	 * <p>
	 * 業務規則：只有在「已通知出貨 (NOTIFIED)」狀態下才允許執行出貨。
	 * </p>
	 * 
	 * @return true 若狀態符合出貨條件
	 */
	public boolean canShip() {
		return "NOTIFIED".equals(this.status);
	}

	/**
	 * 判斷目前訂單是否可以取消
	 * <p>
	 * 業務規則：只要尚未「出貨」且尚未「取消」，皆可執行取消動作。
	 * </p>
	 * 
	 * @return true 若訂單尚在可挽回階段
	 */
	public boolean canCancel() {
		return !"SHIPPED".equals(this.status) && !"CANCELLED".equals(this.status);
	}
}