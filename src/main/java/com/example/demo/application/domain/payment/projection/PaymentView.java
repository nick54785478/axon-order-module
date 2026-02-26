package com.example.demo.application.domain.payment.projection;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payment Read Model (Projection)
 *
 * <p>
 * 充血模型，封裝狀態變更方法，並提供狀態判斷方法。 可在 EventHandler 或 Query Service 使用。
 * </p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PaymentView {

	@Id
	private String paymentId;

	private String orderId;

	private BigDecimal amount;

	private String status;

	/**
	 * 標記付款已建立
	 */
	public void markCreated() {
		if (!"CREATED".equals(this.status)) {
			this.status = "CREATED";
		}
	}

	/**
	 * 標記付款已處理
	 */
	public void markProcessed() {
		if (!"PROCESSED".equals(this.status)) {
			this.status = "PROCESSED";
		}
	}

	/**
	 * 標記付款已取消
	 */
	public void markCancelled() {
		if (!"CANCELLED".equals(this.status)) {
			this.status = "CANCELLED";
		}
	}

	/**
	 * 標記付款已退款
	 */
	public void markRefunded() {
		if (!"REFUNDED".equals(this.status)) {
			this.status = "REFUNDED";
		}
	}

	// 判斷狀態輔助方法
	public boolean canProcess() {
		return "CREATED".equals(this.status);
	}

	public boolean canCancel() {
		return !"CANCELLED".equals(this.status) && !"REFUNDED".equals(this.status);
	}

	public boolean canRefund() {
		return "PROCESSED".equals(this.status);
	}
}