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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OrderView - 訂單詳情 (查詢端模型)
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class OrderView {

	@Id
	private String orderId;

	/**
	 * 使用 ElementCollection 存儲簡單的品項集合
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "order_view_items", joinColumns = @JoinColumn(name = "order_id"))
	private List<OrderItemView> items = new ArrayList<>();

	private BigDecimal amount;

	private String status;

	// ##### 充血模型：狀態變更方法 #####

	public void markShipped() {
		this.status = "SHIPPED";
	}

	public void markNotified() {
		this.status = "NOTIFIED";
	}

	public void markCancelled() {
		this.status = "CANCELLED";
	}

	// ##### 查詢輔助方法 #####

	public boolean canShip() {
		return "NOTIFIED".equals(this.status);
	}

	public boolean canCancel() {
		return !"SHIPPED".equals(this.status) && !"CANCELLED".equals(this.status);
	}
}
