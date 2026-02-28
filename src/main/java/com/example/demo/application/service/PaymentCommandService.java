package com.example.demo.application.service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.payment.aggregate.Payment;
import com.example.demo.application.domain.payment.command.ProcessPaymentCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PaymentCommandService - 支付指令應用服務
 *
 * <p>
 * 本服務負責協調支付相關的寫入操作。作為應用層的門面，它不包含業務邏輯， 僅負責將請求轉發至 {@link Payment} 聚合根。
 * </p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class PaymentCommandService {

	private final CommandGateway commandGateway;

	/**
	 * 執行付款扣款程序
	 * <p>
	 * 當使用者在前端點擊「立即付款」時觸發。此操作會啟動支付聚合根的狀態校驗， 若支付紀錄已過期或已取消，將會拋出異常。
	 * </p>
	 * 
	 * @param paymentId 支付紀錄唯一識別碼
	 * @param orderId   關聯的訂單唯一識別碼
	 * @param amount    本次扣款金額
	 * @return CompletableFuture&lt;Void&gt; 用於追蹤指令執行是否成功
	 */
	public CompletableFuture<Void> processPayment(String paymentId, String orderId, BigDecimal amount) {
		log.info("[Payment Service] 接收付款請求: PaymentId={}, Amount={}", paymentId, amount);

		// 發送指令至 Payment Aggregate 執行扣款邏輯
		return commandGateway.send(new ProcessPaymentCommand(paymentId, orderId, amount));
	}
}