package com.example.demo.infra.processor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.order.event.OrderCancelledEvent;
import com.example.demo.application.domain.order.event.OrderCreatedEvent;
import com.example.demo.application.domain.order.event.OrderNotifiedEvent;
import com.example.demo.application.domain.order.event.OrderShippedEvent;
import com.example.demo.application.domain.order.projection.OrderItemView;
import com.example.demo.application.domain.order.projection.OrderView;
import com.example.demo.infra.persistence.OrderViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderProjectionHandler - 訂單查詢投影處理器
 * <p>
 * 此類別屬於六角形架構中的 Driven Adapter (輸出適配器)。 負責監聽訂單領域事件 (Order Domain
 * Events)，並將資料同步至讀取端資料庫 (OrderView)。 實現了 CQRS 模式中的 Query Side，供前端 API 進行快速查詢。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("order-group") // 指定處理群組，用於管理 Tracking Token 與併發
public class OrderProjectionHandler {

	private final OrderViewRepository repository;

	/**
	 * 處理訂單建立事件 (OrderCreatedEvent) *
	 * <p>
	 * 當新訂單產生時，於資料庫建立對應的 OrderView 紀錄。 包含防禦性設計：若事件中的品項清單為 null (可能為舊版事件)，將初始化為空列表以防止
	 * NPE。
	 * </p>
	 * * @param event 訂單建立事件，包含訂單 ID、品項明細與計算後總額
	 */
	@EventHandler
	public void on(OrderCreatedEvent event) {
		log.info("[Projection] 接收到訂單建立事件: {}", event.orderId());

		OrderView view = new OrderView();
		view.setOrderId(event.orderId());
		view.setAmount(event.amount());
		view.setStatus("CREATED");

		// --- 防禦性設計：處理品項轉換 ---
		// 確保 event.items() 不為 null 才能進行 stream 操作，否則給予空清單
		List<OrderItemView> itemViews = Optional.ofNullable(event.items()).orElse(Collections.emptyList()).stream()
				.map(item -> new OrderItemView(item.productId(), item.quantity(), item.price()))
				.collect(Collectors.toList());

		view.setItems(itemViews);

		repository.save(view);
		log.debug("[Projection] OrderView 已持久化: {}", event.orderId());
	}

	/**
	 * 處理訂單取消事件 (OrderCancelledEvent)
	 * 
	 * @param event 訂單取消事件
	 */
	@EventHandler
	public void on(OrderCancelledEvent event) {
		log.info("[Projection] 更新 OrderView 為 CANCELLED: {}", event.orderId());

		repository.findById(event.orderId()).ifPresentOrElse(view -> {
			view.markCancelled(); // 使用充血模型內建的狀態變更邏輯
			repository.save(view);
		}, () -> log.warn("[Projection] 找不到欲取消的訂單: {}", event.orderId()));
	}

	/**
	 * 處理通知出貨事件 (OrderNotifiedEvent)
	 * <p>
	 * 通常由 Saga 在支付成功後觸發，代表訂單進入待出貨階段。
	 * </p>
	 * * @param event 通知出貨事件
	 */
	@EventHandler
	public void on(OrderNotifiedEvent event) {
		log.info("[Projection] 更新 OrderView 為 NOTIFIED: {}", event.orderId());

		repository.findById(event.orderId()).ifPresent(view -> {
			view.markNotified(); // 同步更新為通知出貨狀態
			repository.save(view);
		});
	}

	/**
	 * 處理訂單出貨完成事件 (OrderShippedEvent)
	 * 
	 * @param event 訂單出貨事件
	 */
	@EventHandler
	public void on(OrderShippedEvent event) {
		log.info("[Projection] 更新 OrderView 為 SHIPPED: {}", event.orderId());

		repository.findById(event.orderId()).ifPresentOrElse(view -> {
			view.markShipped(); // 更新為最終完成狀態
			repository.save(view);
		}, () -> log.warn("[Projection] 找不到欲更新出貨狀態的訂單: {}", event.orderId()));
	}
}