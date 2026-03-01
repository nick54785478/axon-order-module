package com.example.demo.iface.rest;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.domain.order.command.ConfirmOrderShipmentCommand;
import com.example.demo.application.domain.order.command.CreateOrderCommand;
import com.example.demo.application.service.OrderCommandService;
import com.example.demo.application.service.OrderQueryService;
import com.example.demo.application.shared.query.FindAllOrdersQuery;
import com.example.demo.application.shared.query.GetOrderPaymentsQuery;
import com.example.demo.application.shared.query.GetOrderQuery;
import com.example.demo.iface.dto.req.CreateOrderResource;
import com.example.demo.iface.dto.res.OrderCancelledResource;
import com.example.demo.iface.dto.res.OrderCreatedResource;
import com.example.demo.iface.dto.res.OrderQueriedResource;
import com.example.demo.iface.dto.res.OrderShipmentConfirmedResource;
import com.example.demo.iface.dto.res.OrdersQueriedResource;
import com.example.demo.iface.dto.res.PaymentsQueriedResource;
import com.example.demo.infra.mapper.OrderMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/orders")
public class OrderController {

	private final OrderMapper orderMapper;
	private final OrderQueryService queryService;
	private final OrderCommandService commandService;

	/**
	 * 建立訂單
	 */
	@PostMapping("")
	public CompletableFuture<ResponseEntity<OrderCreatedResource>> createOrder(
			@RequestBody CreateOrderResource resource) {
		// 利用我們之前在 Command Record 寫的邏輯：若沒傳 ID 就自動產生
		CreateOrderCommand command = orderMapper.transformACL(resource);
		String orderId = command.orderId(); // 從 Command 拿 ID，確保一致性
		return commandService.createOrder(command).thenApply(result -> ResponseEntity.status(HttpStatus.CREATED)
				.body(new OrderCreatedResource("200", "建立訂單成功", orderId))).exceptionally(ex -> {
					log.error("[API] 建立訂單失敗: {}", ex.getMessage());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(new OrderCreatedResource("400", "金額或品項錯誤: " + ex.getCause().getMessage(), null));
				});
	}

	/**
	 * 手動確認出貨 API
	 */
	@PutMapping("/{orderId}/ship")
	public CompletableFuture<ResponseEntity<OrderShipmentConfirmedResource>> confirmShipment(
			@PathVariable String orderId) {
		ConfirmOrderShipmentCommand command = new ConfirmOrderShipmentCommand(orderId);

		return commandService.confirmShipment(command)
				.thenApply(result -> ResponseEntity.ok(new OrderShipmentConfirmedResource("200", "出貨成功", orderId)))
				.exceptionally(ex -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
						new OrderShipmentConfirmedResource("400", "出貨失敗: " + ex.getCause().getMessage(), orderId)));
	}

	/**
	 * 手動取消訂單 API
	 * <p>
	 * 業務規則：已出貨不可取消，已取消不可再次取消
	 * </p>
	 */
	@PutMapping("/{orderId}/cancel")
	public CompletableFuture<ResponseEntity<OrderCancelledResource>> cancelOrder(@PathVariable String orderId) {
		log.info("[API] 收到訂單取消請求: {}", orderId);
		return commandService.cancelOrder(orderId)
				.thenApply(result -> ResponseEntity.ok(new OrderCancelledResource("200", "訂單已成功提交取消請求", orderId)))
				.exceptionally(ex -> {
					// 處理 Aggregate 拋出的 IllegalStateException (如：已出貨不可取消)
					log.error("[API] 取消訂單失敗: {}", ex.getMessage());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(new OrderCancelledResource("400", "無法取消訂單: " + ex.getCause().getMessage(), orderId));
				});
	}

	/**
	 * API: 獲取訂單清單
	 */
	@GetMapping("/list")
	public CompletableFuture<ResponseEntity<OrdersQueriedResource>> findAll() {
		return queryService.getAllOrders(new FindAllOrdersQuery())
				.thenApply(list -> new OrdersQueriedResource("200", "查詢成功", list)).thenApply(ResponseEntity::ok)
				.exceptionally(ex -> {
					// 發生錯誤時的回傳 (例如 500 Error)
					OrdersQueriedResource errorRes = new OrdersQueriedResource("500", "Error: " + ex.getMessage(),
							new ArrayList<>());
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
				});
	}

	/**
	 * API: 獲取特定訂單細節
	 */
	@GetMapping("/{orderId}")
	public CompletableFuture<ResponseEntity<OrderQueriedResource>> getOrder(@PathVariable String orderId) {
		GetOrderQuery query = new GetOrderQuery(orderId);
		return queryService.getOrder(query).thenApply(data -> new OrderQueriedResource("200", "查詢成功", data))
				.thenApply(ResponseEntity::ok).exceptionally(ex -> {
					OrderQueriedResource errorRes = new OrderQueriedResource("500", "Error: " + ex.getMessage(), null);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
				});
	}

	/**
	 * API: 獲取特定訂單的付款狀態
	 */
	@GetMapping("/{orderId}/payments")
	public CompletableFuture<ResponseEntity<PaymentsQueriedResource>> getPayments(@PathVariable String orderId) {
		return queryService.getPayments(new GetOrderPaymentsQuery(orderId))
				.thenApply(payments -> new PaymentsQueriedResource("200", "查詢成功", payments))
				.thenApply(ResponseEntity::ok).exceptionally(ex -> {
					PaymentsQueriedResource errorRes = new PaymentsQueriedResource("500", "Error: " + ex.getMessage(),
							null);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
				});
	}
}