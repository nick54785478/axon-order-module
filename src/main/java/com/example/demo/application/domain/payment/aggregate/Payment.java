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
 * Payment Aggregate Root - 支付聚合根 *
 * <p>
 * 負責管理支付紀錄的生命週期。採用 Event Sourcing 設計， 所有的狀態變更都必須透過 Event 發生。
 * 內建狀態機防禦，確保「已取消」或「已退款」的支付無法被再次執行。
 * </p>
 */
@Slf4j
@Aggregate
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

	/**
	 * 支付唯一識別碼
	 */
	@AggregateIdentifier
	private String paymentId;

	/**
	 * 支付狀態 (CREATED, PROCESSED, CANCELLED, REFUNDED)
	 */
	private String status;

	/**
	 * 建立支付紀錄
	 */
	@CommandHandler
	public Payment(CreatePaymentCommand command) {
		log.info("[Payment] 建立支付紀錄: {}", command.paymentId());
		AggregateLifecycle.apply(new PaymentCreatedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	/**
	 * 執行支付 (由 API 觸發)
	 * 
	 * @throws IllegalStateException 若支付狀態不是 CREATED (例如已過期)
	 */
	@CommandHandler
	public void handle(ProcessPaymentCommand command) {
		log.info("[Payment] 執行支付扣款: {}", command.paymentId());

		// 核心業務校驗：防止過期或已取消的紀錄進行支付
		if (!"CREATED".equals(this.status)) {
			throw new IllegalStateException("無法執行支付：該紀錄目前狀態為 " + this.status);
		}

		AggregateLifecycle.apply(new PaymentProcessedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	/**
	 * 取消支付 (由 Saga 同步觸發)
	 */
	@CommandHandler
	public void handle(CancelPaymentCommand command) {
		log.info("[Payment] 取消支付紀錄: {}", command.paymentId());

		// 如果已經付過錢了，就不應該被取消 (應走退款)
		if ("PROCESSED".equals(this.status)) {
			log.warn("支付已完成，忽略取消指令。");
			return;
		}

		AggregateLifecycle.apply(new PaymentCancelledEvent(command.paymentId(), command.orderId()));
	}

	/**
	 * 執行退款
	 */
	@CommandHandler
	public void handle(RefundPaymentCommand command) {
		log.info("[Payment] 執行退款: {}", command.paymentId());

		if (!"PROCESSED".equals(this.status)) {
			throw new IllegalStateException("只有已支付完成的紀錄可以退款");
		}

		AggregateLifecycle.apply(new PaymentRefundedEvent(command.paymentId(), command.orderId(), command.amount()));
	}

	// ##### Event Sourcing Handlers #####

	@EventSourcingHandler
	public void on(PaymentCreatedEvent event) {
		this.paymentId = event.paymentId();
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