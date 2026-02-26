package com.example.demo.iface.rest;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.PaymentCommandService;
import com.example.demo.iface.dto.req.ProcessPaymentResource;
import com.example.demo.iface.dto.res.PaymentProcessedResource;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payments")
@AllArgsConstructor
public class PaymentController {

	private final PaymentCommandService paymentCommandService;

	/**
	 * 執行付款 API
	 * 
	 * @param paymentId 支付 ID (下單時由 Saga 產生，可透過查詢訂單獲得)
	 */
	@PutMapping("/{paymentId}/process")
	public CompletableFuture<ResponseEntity<PaymentProcessedResource>> processPayment(@PathVariable String paymentId,
			@RequestBody ProcessPaymentResource resource) {
		log.info("[API] 收到付款請求: PaymentId={}, OrderId={}", paymentId, resource.orderId());
		return paymentCommandService.processPayment(paymentId, resource.orderId(), resource.amount())
				.thenApply(result -> ResponseEntity.ok(new PaymentProcessedResource("200", "付款處理指令已送出", paymentId)))
				.exceptionally(ex -> {
					// 處理 Aggregate 拋出的 IllegalStateException (如：已出貨不可取消)
					log.error("[API] 取消訂單失敗: {}", ex.getMessage());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
							new PaymentProcessedResource("400", "付款失敗: " + ex.getCause().getMessage(), paymentId));
				});

	}
}