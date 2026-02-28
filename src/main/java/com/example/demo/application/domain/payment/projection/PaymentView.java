package com.example.demo.application.domain.payment.projection;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PaymentView - 支付讀取模型 (Read Model)
 *
 * <p>
 * 此實體屬於 Infrastructure Layer，是支付資訊在資料庫中的持久化表示。
 * 採用「充血模型」設計，將狀態變更與邏輯判斷封裝於實體內部，而非散落在 Service 中。
 * </p>
 *
 * <h3>設計細節：</h3>
 * <ul>
 * <li><b>冪等性保護：</b> markXXX 方法皆內建狀態檢查，確保事件重播 (Replay) 時不會造成重複更新。</li>
 * <li><b>業務輔助：</b> 提供 canXXX 方法，協助查詢端判斷該支付紀錄的後續可用操作。</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "payment_view") // 建議顯式指定資料表名稱
public class PaymentView {

	/**
	 * 支付唯一識別碼 (對應 Aggregate 的 PaymentId)
	 */
	@Id
	private String paymentId;

	/**
	 * 關聯的訂單 ID
	 */
	private String orderId;

	/**
	 * 支付金額
	 */
	private BigDecimal amount;

	/**
	 * * 支付狀態
	 * <p>
	 * 狀態流轉：CREATED -> PROCESSED -> REFUNDED (或 CREATED -> CANCELLED)
	 * </p>
	 */
	private String status;

	// ##### 1. 狀態變更方法 (State Mutators) #####

	/**
	 * 標記付款已建立
	 * <p>
	 * 當接收到 PaymentCreatedEvent 時呼叫，初始化支付紀錄。
	 * </p>
	 */
	public void markCreated() {
		if (!"CREATED".equals(this.status)) {
			this.status = "CREATED";
		}
	}

	/**
	 * 標記付款已處理 (支付成功)
	 * <p>
	 * 當扣款成功後呼叫。內建冪等性檢查，避免重複處理事件。
	 * </p>
	 */
	public void markProcessed() {
		if (!"PROCESSED".equals(this.status)) {
			this.status = "PROCESSED";
		}
	}

	/**
	 * 標記付款已取消
	 * <p>
	 * 當訂單失效或超時，支付通道關閉時呼叫。
	 * </p>
	 */
	public void markCancelled() {
		if (!"CANCELLED".equals(this.status)) {
			this.status = "CANCELLED";
		}
	}

	/**
	 * 標記付款已退款
	 * <p>
	 * 當發生逆向物流或補償時，標記該筆支付已完成退款沖銷。
	 * </p>
	 */
	public void markRefunded() {
		if (!"REFUNDED".equals(this.status)) {
			this.status = "REFUNDED";
		}
	}

	// ##### 2. 狀態判斷輔助方法 (Business Predicates) #####

	/**
	 * 判斷是否可執行支付扣款
	 * 
	 * @return true 若狀態為已建立且尚未支付
	 */
	public boolean canProcess() {
		return "CREATED".equals(this.status);
	}

	/**
	 * 判斷是否可執行取消
	 * 
	 * @return true 若尚未進入最終狀態 (已取消或已退款)
	 */
	public boolean canCancel() {
		return !"CANCELLED".equals(this.status) && !"REFUNDED".equals(this.status);
	}

	/**
	 * 判斷是否可執行退款
	 * 
	 * @return true 僅在支付成功 (PROCESSED) 的情況下允許退款
	 */
	public boolean canRefund() {
		return "PROCESSED".equals(this.status);
	}
}