package com.example.demo.application.domain.order.projection;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Order Read Model (Projection)
 *
 * <p>
 * 充血模型，封裝狀態變更方法，並提供狀態判斷方法。 可在 EventHandler 或 Query Service 使用。
 * </p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class OrderView {

	@Id
	private String orderId;

	private BigDecimal amount;

	private String status;
	
	

	/**
	 * 標記訂單為出貨
	 * <p>
	 * 具備 Idempotent 特性：若已出貨則不重複操作
	 * </p>
	 */
	public void markShipped() {
		if (!"SHIPPED".equals(this.status)) {
			this.status = "SHIPPED";
		}
	}
	
	/**
	 * 標記訂單為出貨
	 * <p>
	 * 具備 Idempotent 特性：若已出貨則不重複操作
	 * </p>
	 */
	public void markNotified() {
		if (!"NOTIFIED".equals(this.status)) {
			this.status = "NOTIFIED";
		}
	}

	/**
	 * 標記訂單為取消
	 * <p>
	 * 具備 Idempotent 特性：若已取消則不重複操作
	 * </p>
	 */
	public void markCancelled() {
		if (!"CANCELLED".equals(this.status)) {
			this.status = "CANCELLED";
		}
	}

	/**
	 * 是否可以出貨
	 */
	public boolean canShip() {
		return "CREATED".equals(this.status);
	}

	/**
	 * 是否可以取消
	 */
	public boolean canCancel() {
		return "CREATED".equals(this.status);
	}
}
