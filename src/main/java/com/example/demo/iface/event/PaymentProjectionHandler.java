package com.example.demo.iface.event;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.payment.event.PaymentCancelledEvent;
import com.example.demo.application.domain.payment.event.PaymentCreatedEvent;
import com.example.demo.application.domain.payment.event.PaymentProcessedEvent;
import com.example.demo.application.domain.payment.event.PaymentRefundedEvent;
import com.example.demo.application.domain.payment.projection.PaymentView;
import com.example.demo.infra.persistence.PaymentViewRepository;

import lombok.AllArgsConstructor;

/**
 * PaymentProjectionHandler - 支付查詢投影處理器 *
 * <p>
 * 作為 Driven Adapter (輸出適配器)，負責監聽支付模組的領域事件， 並將變更持久化到 MySQL 資料庫中，供前端 Query API
 * 使用。
 * </p>
 */
@Component
@AllArgsConstructor
public class PaymentProjectionHandler {

	private final PaymentViewRepository repository;

	/**
	 * 處理付款建立，初始化 View
	 */
	@EventHandler
	public void on(PaymentCreatedEvent event) {
		PaymentView view = new PaymentView();
		view.setPaymentId(event.paymentId());
		view.setOrderId(event.orderId());
		view.setAmount(event.amount());
		view.markCreated(); // 使用充血模型方法
		repository.save(view);
	}

	/**
	 * 處理付款成功更新
	 */
	@EventHandler
	public void on(PaymentProcessedEvent event) {
		repository.findById(event.paymentId()).ifPresent(view -> {
			view.markProcessed();
			repository.save(view);
		});
	}

	/**
	 * 處理付款取消更新 (例如訂單超時自動取消)
	 */
	@EventHandler
	public void on(PaymentCancelledEvent event) {
		repository.findById(event.paymentId()).ifPresent(view -> {
			view.markCancelled();
			repository.save(view);
		});
	}

	/**
	 * 處理退款更新
	 */
	@EventHandler
	public void on(PaymentRefundedEvent event) {
		repository.findById(event.paymentId()).ifPresent(view -> {
			view.markRefunded();
			repository.save(view);
		});
	}
}