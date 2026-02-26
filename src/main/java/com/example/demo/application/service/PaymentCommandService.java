package com.example.demo.application.service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.payment.command.ProcessPaymentCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentCommandService {

	private final CommandGateway commandGateway;

	/**
	 * 進行付款
	 * 
	 * @param paymentId 付款唯一值
	 * @param orderId   訂單唯一值
	 * @param amount    金額
	 */
	public CompletableFuture<Void> processPayment(String paymentId, String orderId, BigDecimal amount) {
		// 使用者點擊付款按鈕後，觸發此指令
		return commandGateway.send(new ProcessPaymentCommand(paymentId, orderId, amount));
	}
}