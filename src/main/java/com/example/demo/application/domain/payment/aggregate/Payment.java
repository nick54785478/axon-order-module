package com.example.demo.application.domain.payment.aggregate;

import java.math.BigDecimal;

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
import com.example.demo.application.shared.exception.InsufficientFundsException;

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
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

	@AggregateIdentifier
	private String paymentId;

	private String status;

	/** 支付金額：用於扣款時的金額一致性校驗 */
	private BigDecimal amount;

	// ##### 1. 建立階段 #####

	@CommandHandler
	public Payment(CreatePaymentCommand command) {
		log.info("[Payment] 建立支付紀錄，金額: {}", command.amount());
		AggregateLifecycle.apply(new PaymentCreatedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	@EventSourcingHandler
	public void on(PaymentCreatedEvent event) {
		this.paymentId = event.paymentId();
		this.amount = event.amount(); // 關鍵：記錄下這筆支付應扣多少錢
		this.status = "CREATED";
	}

	// ##### 2. 扣款階段 (整合校驗邏輯) #####

	@CommandHandler
	public void handle(ProcessPaymentCommand command) {
		log.info("[Payment] 執行支付扣款校驗: {}", command.paymentId());

		// 防線 1：狀態檢查
		if (!"CREATED".equals(this.status)) {
			throw new IllegalStateException("無法執行支付：目前狀態為 " + this.status);
		}

		// 防線 2：金額一致性校驗 (防止前端或外部惡意竄改支付金額)
		if (this.amount.compareTo(command.amount()) != 0) {
			log.error("[Payment] 金額不符！預期: {}, 實際: {}", this.amount, command.amount());
			throw new IllegalArgumentException("支付失敗：交易金額與紀錄不符");
		}

		// 防線 3：餘額校驗 (模擬業務規則)
		// 這裡模擬：如果單筆支付超過 100,000 元，則判定為餘額不足
		if (command.amount().compareTo(new BigDecimal("100000")) > 0) {
			throw new InsufficientFundsException("支付失敗：餘額不足 (單筆限額 10 萬)");
		}

		AggregateLifecycle.apply(new PaymentProcessedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	@EventSourcingHandler
	public void on(PaymentProcessedEvent event) {
		this.status = "PROCESSED";
	}

	// ##### 3. 取消與退款 (保持原樣) #####

	@CommandHandler
	public void handle(CancelPaymentCommand command) {
		if ("PROCESSED".equals(this.status))
			return;
		AggregateLifecycle.apply(new PaymentCancelledEvent(command.paymentId(), command.orderId()));
	}

	@EventSourcingHandler
	public void on(PaymentCancelledEvent event) {
		this.status = "CANCELLED";
	}

	@CommandHandler
	public void handle(RefundPaymentCommand command) {
		if (!"PROCESSED".equals(this.status))
			throw new IllegalStateException("未支付不可退款");
		AggregateLifecycle.apply(new PaymentRefundedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	@EventSourcingHandler
	public void on(PaymentRefundedEvent event) {
		this.status = "REFUNDED";
	}
}