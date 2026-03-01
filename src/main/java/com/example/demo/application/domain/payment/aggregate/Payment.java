package com.example.demo.application.domain.payment.aggregate;

import java.math.BigDecimal;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateVersion;
import org.axonframework.spring.stereotype.Aggregate;

import com.example.demo.application.domain.payment.command.CancelPaymentCommand;
import com.example.demo.application.domain.payment.command.CreatePaymentCommand;
import com.example.demo.application.domain.payment.command.ProcessPaymentCommand;
import com.example.demo.application.domain.payment.command.RefundPaymentCommand;
import com.example.demo.application.domain.payment.event.PaymentCancelledEvent;
import com.example.demo.application.domain.payment.event.PaymentCreatedEvent;
import com.example.demo.application.domain.payment.event.PaymentProcessedEvent;
import com.example.demo.application.domain.payment.event.PaymentRefundedEvent;
import com.example.demo.application.shared.exception.InsufficientFundsException;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment Aggregate Root - 支付聚合根
 *
 * <p>
 * 職責：管理支付生命週期。 在逆向流程中，本聚合根負責執行「退款」動作，無論該動作是由「訂單取消」還是「訂單退貨」所觸發。
 * </p>
 */
@Slf4j
@Aggregate
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

	@AggregateIdentifier
	private String paymentId;

	/**
	 * 樂觀鎖版本號 確保支付狀態的變更在併發環境下是安全的。
	 */
	@AggregateVersion
	private Long version;

	private String status;

	private BigDecimal amount;

	// ##### 1. 建立與扣款 (保持專業校驗) #####

	@CommandHandler
	public Payment(CreatePaymentCommand command) {
		log.info("[Payment] 建立支付紀錄: {}, 金額: {}", command.paymentId(), command.amount());
		AggregateLifecycle.apply(new PaymentCreatedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	/**
	 * 處理付款扣款指令
	 * 
	 * <pre>
	 * 執行三重防禦： 
	 * 	1. 狀態檢查：必須是 CREATED。 
	 * 	2. 金額一致性：傳入金額必須等於紀錄中的應付金額。
	 *	3. 餘額/限額模擬：檢查是否超過單筆限額。
	 *  
	 * </pre>
	 */
	@CommandHandler
	public void handle(ProcessPaymentCommand command) {
		log.info("[Payment] 執行支付扣款校驗: {}", command.paymentId());

		// 防線 1：狀態檢查
		if (!"CREATED".equals(this.status)) {
			throw new IllegalStateException("無法執行支付：目前狀態為 " + this.status);
		}

		// 防線 2：金額一致性校驗
		// 使用 compareTo == 0 來比對 BigDecimal，避免精度或尾隨零的問題
		if (this.amount == null || this.amount.compareTo(command.amount()) != 0) {
			log.error("[Payment] 金額不符！預期(應付): {}, 實際(傳入): {}", this.amount, command.amount());
			throw new IllegalArgumentException("支付失敗：交易金額 (" + command.amount() + ") 與訂單應付金額 (" + this.amount + ") 不符");
		}

		// 防線 3：餘額/風險校驗 (模擬業務規則)
		// 這裡模擬：如果單筆支付超過 100,000 元，則判定為餘額不足或超過限額
		if (command.amount().compareTo(new BigDecimal("100000")) > 0) {
			throw new InsufficientFundsException("支付失敗：餘額不足或超過單筆交易限額 (10 萬)");
		}

		// 通過所有校驗，發布成功事件
		AggregateLifecycle.apply(new PaymentProcessedEvent(command.paymentId(), command.orderId(), this.amount // 使用聚合根內部紀錄的權威金額
		));
	}

	// ##### 2. 逆向流程：取消與退款 (強化穩健性) #####

	/**
	 * 處理支付取消指令 (僅限未支付狀態)
	 */
	@CommandHandler
	public void handle(CancelPaymentCommand command) {
		// 冪等性處理：若已經是取消或退款狀態，則不重複處理
		if ("CANCELLED".equals(this.status) || "REFUNDED".equals(this.status)) {
			return;
		}

		// 若已支付，則不允許直接取消（必須走 Refund 流程）
		if ("PROCESSED".equals(this.status)) {
			log.warn("[Payment] 支付已完成，不可直接取消。請執行退款流程。");
			return;
		}

		AggregateLifecycle.apply(new PaymentCancelledEvent(command.paymentId(), command.orderId()));
	}

	/**
	 * 執行退款指令
	 * <p>
	 * 觸發場景： 1. 訂單已支付但被取消 (OrderCancelledEvent) 2. 訂單已完成但執行退貨 (OrderReturnedEvent)
	 * </p>
	 */
	@CommandHandler
	public void handle(RefundPaymentCommand command) {
		log.info("[Payment] 處理退款請求: {}, 目前狀態: {}", command.paymentId(), this.status);

		// 1. 冪等性檢查：若已經退款過，則直接返回，避免重複發布事件
		if ("REFUNDED".equals(this.status)) {
			log.info("[Payment] 此筆支付已完成退款，略過重複操作。");
			return;
		}

		// 2. 狀態防禦：僅已支付 (PROCESSED) 的紀錄可執行退款
		if (!"PROCESSED".equals(this.status)) {
			throw new IllegalStateException("退款失敗：目前狀態為 " + this.status + "，僅已支付紀錄可執行退款");
		}

		// 3. 發布退款事實
		AggregateLifecycle.apply(new PaymentRefundedEvent(command.paymentId(), command.orderId(), this.amount));
	}

	// ##### 3. 事件溯源處理 (Event Sourcing Handlers) #####

	@EventSourcingHandler
	public void on(PaymentCreatedEvent event) {
		this.paymentId = event.paymentId();
		this.amount = event.amount();
		this.status = "CREATED";
	}

	@EventSourcingHandler
	public void on(PaymentProcessedEvent event) {
		this.status = "PROCESSED";
	}

	@EventSourcingHandler
	public void on(PaymentCancelledEvent event) {
		this.status = "CANCELLED";
	}

	@EventSourcingHandler
	public void on(PaymentRefundedEvent event) {
		this.status = "REFUNDED";
	}
}