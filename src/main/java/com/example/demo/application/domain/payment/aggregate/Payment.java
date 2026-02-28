package com.example.demo.application.domain.payment.aggregate;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import com.example.demo.application.domain.payment.command.CancelPaymentCommand;
import com.example.demo.application.domain.payment.command.CreatePaymentCommand;
import com.example.demo.application.domain.payment.command.ProcessPaymentCommand;
import com.example.demo.application.domain.payment.command.RefundPaymentCommand;
import com.example.demo.application.domain.payment.event.PaymentCancelledEvent;
import com.example.demo.application.domain.payment.event.PaymentCreatedEvent;
import com.example.demo.application.domain.payment.event.PaymentProcessedEvent;
import com.example.demo.application.domain.payment.event.PaymentRefundedEvent;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment Aggregate Root - 支付聚合根
 *
 * <p>
 * 此聚合根負責管理單筆支付紀錄的完整生命週期，是金流操作的強一致性邊界。
 * </p>
 * *
 * <h3>設計核心：</h3>
 * <ul>
 * <li><b>事件溯源 (Event Sourcing)：</b> 狀態完全由 {@link EventSourcingHandler}
 * 根據歷史事實重建，不保存任何中間計算狀態。</li>
 * <li><b>狀態機防禦：</b> 嚴格限制狀態跳轉（如：已支付不可取消、未支付不可退款），防止非法業務操作。</li>
 * <li><b>分散式協作：</b> 通常與 Order Saga 配合，作為 Saga 流程中的一個任務執行單元。</li>
 * </ul>
 */
@Slf4j
@Aggregate
@NoArgsConstructor // Axon 框架重建聚合根所需
@AllArgsConstructor
public class Payment {

	/**
	 * 支付唯一識別碼
	 * <p>
	 * 對應資料庫中的支付主鍵，亦為 Axon 指令路由的基準。
	 * </p>
	 */
	@AggregateIdentifier
	private String paymentId;

	/**
	 * 支付狀態
	 * <p>
	 * 可選值：CREATED (已建立), PROCESSED (已支付), CANCELLED (已取消), REFUNDED (已退款)。
	 * </p>
	 * <p>
	 * 註：實務上建議使用 Enum 以提升類型安全性。
	 * </p>
	 */
	private String status;

	// ##### 1. 建立階段 (Initialization) #####

	/**
	 * 【Constructor Command Handler】建立支付紀錄
	 * <p>
	 * 當系統接收到支付請求時，初始化支付生命週期。
	 * </p>
	 * * @param command 包含支付 ID、訂單 ID 與金額的指令
	 */
	@CommandHandler
	public Payment(CreatePaymentCommand command) {
		log.info("[Payment] 接收到 CreatePaymentCommand，ID: {}", command.paymentId());

		// 發布事實：支付紀錄已初始化
		AggregateLifecycle.apply(new PaymentCreatedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	/**
	 * 響應支付建立事件
	 */
	@EventSourcingHandler
	public void on(PaymentCreatedEvent event) {
		this.paymentId = event.paymentId();
		this.status = "CREATED"; // 初始狀態
	}

	// ##### 2. 扣款階段 (Execution) #####

	/**
	 * 【Command Handler】執行支付扣款
	 * <p>
	 * 此為金流變動的關鍵動作。必須確保紀錄處於 CREATED 狀態，避免重複扣款或對已失效的紀錄扣款。
	 * </p>
	 * 
	 * @param command 執行支付指令
	 * 
	 * @throws IllegalStateException 若目前狀態不允許執行支付 (如已取消或已完成)
	 */
	@CommandHandler
	public void handle(ProcessPaymentCommand command) {
		log.info("[Payment] 執行支付扣款，ID: {}", command.paymentId());

		// 核心業務校驗：狀態檢查
		if (!"CREATED".equals(this.status)) {
			throw new IllegalStateException("無法執行支付：目前狀態為 " + this.status + "，僅能處理 CREATED 狀態的紀錄");
		}

		// 模擬扣款邏輯通過後，發布支付成功事實
		AggregateLifecycle.apply(new PaymentProcessedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	/**
	 * 響應支付成功事件
	 */
	@EventSourcingHandler
	public void on(PaymentProcessedEvent event) {
		this.status = "PROCESSED";
	}

	// ##### 3. 取消與補償階段 (Compensation) #####

	/**
	 * 【Command Handler】取消支付紀錄
	 * <p>
	 * 通常由 Saga 流程因訂單超時或手動撤單而觸發。
	 * </p>
	 * 
	 * @param command 取消支付指令
	 */
	@CommandHandler
	public void handle(CancelPaymentCommand command) {
		log.info("[Payment] 嘗試取消支付紀錄: {}", command.paymentId());

		// 冪等性與狀態守護：若已支付完成，則不可直接「取消」，應由後續「退款」流程處理
		if ("PROCESSED".equals(this.status)) {
			log.warn("[Payment] 支付 ID {} 已完成付款，忽略取消指令 (應走退款流程)。", command.paymentId());
			return;
		}

		AggregateLifecycle.apply(new PaymentCancelledEvent(command.paymentId(), command.orderId()));
	}

	/**
	 * 響應取消事件
	 */
	@EventSourcingHandler
	public void on(PaymentCancelledEvent event) {
		this.status = "CANCELLED";
	}

	/**
	 * 【Command Handler】執行退款
	 * <p>
	 * 用於處理已支付完成後的逆向物流或售後流程。
	 * </p>
	 * 
	 * @param command 退款指令
	 * 
	 * @throws IllegalStateException 若尚未支付成功則無法退款
	 */
	@CommandHandler
	public void handle(RefundPaymentCommand command) {
		log.info("[Payment] 處理退款請求: {}", command.paymentId());

		// 業務校驗：只有已成功的支付可以退款
		if (!"PROCESSED".equals(this.status)) {
			throw new IllegalStateException("無法退款：該紀錄尚未支付完成 (目前狀態: " + this.status + ")");
		}

		AggregateLifecycle.apply(new PaymentRefundedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	/**
	 * 響應退款事件
	 */
	@EventSourcingHandler
	public void on(PaymentRefundedEvent event) {
		this.status = "REFUNDED";
	}
}