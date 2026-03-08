package com.example.demo.infra.processor;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.payment.event.PaymentCancelledEvent;
import com.example.demo.application.domain.payment.event.PaymentCreatedEvent;
import com.example.demo.application.domain.payment.event.PaymentProcessedEvent;
import com.example.demo.application.domain.payment.event.PaymentRefundedEvent;
import com.example.demo.infra.persistence.PaymentViewRepository;
import com.example.demo.infra.projection.payment.PaymentView;

import lombok.AllArgsConstructor;

/**
 * PaymentProjectionHandler - 支付查詢投影處理器 *
 * <p>
 * 此類別屬於六角形架構中的 Driven Adapter (輸出適配器)。 專門負責監聽「支付模組」產生的領域事件 (Domain
 * Events)，並將變更同步至 MySQL 資料庫中的 PaymentView 讀取模型。
 * </p>
 * *
 * <h3>設計特色：</h3>
 * <ul>
 * <li><b>充血模型應用：</b> 透過呼叫 PaymentView 內建的狀態變更方法 (如
 * markProcessed)，確保狀態轉換邏輯封裝在實體中。</li>
 * <li><b>最終一致性：</b> 確保金流狀態能隨著 Event Store 中的事件流轉，即時反映在查詢端。</li>
 * <li><b>冪等性處理：</b> 透過 JPA 的 findById 與狀態校驗，確保重複接收事件時不會產生錯誤的狀態跳轉。</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class PaymentProjectionHandler {

	/**
	 * 支付視圖資料庫存取接口
	 */
	private final PaymentViewRepository repository;

	/**
	 * 處理「付款建立事件」(PaymentCreatedEvent)
	 * <p>
	 * 當 Saga 流程發起支付申請時，於資料庫初始化一筆「CREATED」狀態的支付紀錄。
	 * </p>
	 * 
	 * @param event 包含支付 ID、訂單 ID 與支付金額的事件
	 */
	@EventHandler
	public void on(PaymentCreatedEvent event) {
		// 建立新的投影實體
		PaymentView view = new PaymentView();
		view.setPaymentId(event.paymentId());
		view.setOrderId(event.orderId());
		view.setAmount(event.amount());

		// 使用實體內部的業務方法設定初始狀態
		view.markCreated();

		repository.save(view);
	}

	/**
	 * 處理「付款成功事件」(PaymentProcessedEvent)
	 * <p>
	 * 當外部支付系統回傳成功，且 Payment 聚合根處理完成後觸發。將狀態更新為「PROCESSED」。
	 * </p>
	 * 
	 * @param event 包含支付 ID 的成功事件
	 */
	@EventHandler
	public void on(PaymentProcessedEvent event) {
		// 查找既有紀錄並更新狀態
		repository.findById(event.paymentId()).ifPresent(view -> {
			view.markProcessed();
			repository.save(view);
		});
	}

	/**
	 * 處理「付款取消事件」(PaymentCancelledEvent)
	 * <p>
	 * 通常發生在： 1. 訂單支付超時，由 Saga 自動觸發。 2. 使用者手動取消訂單時，同步關閉支付通道。
	 * </p>
	 * 
	 * @param event 包含支付 ID 的取消事件
	 */
	@EventHandler
	public void on(PaymentCancelledEvent event) {
		repository.findById(event.paymentId()).ifPresent(view -> {
			view.markCancelled();
			repository.save(view);
		});
	}

	/**
	 * 處理「退款完成事件」(PaymentRefundedEvent)
	 * <p>
	 * 當訂單在支付後因出貨失敗或其他原因需要補償時，將紀錄標記為「REFUNDED」。
	 * </p>
	 * 
	 * @param event 包含支付 ID 與退款金額的事件
	 */
	@EventHandler
	public void on(PaymentRefundedEvent event) {
		repository.findById(event.paymentId()).ifPresent(view -> {
			view.markRefunded();
			repository.save(view);
		});
	}
}