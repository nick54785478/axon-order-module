package com.example.demo.infra.processor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.SequenceNumber;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.order.event.OrderCancelledEvent;
import com.example.demo.application.domain.order.event.OrderCreatedEvent;
import com.example.demo.application.domain.order.event.OrderNotifiedEvent;
import com.example.demo.application.domain.order.event.OrderReturnedEvent;
import com.example.demo.application.domain.order.event.OrderShippedEvent;
import com.example.demo.application.domain.order.projection.OrderItemView;
import com.example.demo.application.domain.order.projection.OrderView;
import com.example.demo.infra.persistence.OrderViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderProjectionHandler - 訂單查詢投影處理器
 * <p>
 * 此類別負責監聽訂單領域事件，並將資料同步至 MySQL (OrderView)。 核心升級：透過 {@code @SequenceNumber}
 * 獲取事件序列號，確保讀取模型的版本與聚合根完全一致， 從而支援前端 API 的樂觀鎖校驗。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("order-group")
public class OrderProjectionHandler {

	private final OrderViewRepository repository;

	/**
	 * 處理訂單建立事件 (OrderCreatedEvent)
	 * <p>
	 * 初始版本號為 0。
	 * </p>
	 * * @param event 訂單建立事實
	 * 
	 * @param version 來自 Axon Event Store 的序列號 (@SequenceNumber)
	 */
	@EventHandler
	public void on(OrderCreatedEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 建立訂單視圖: {}, Version: {}", event.orderId(), version);

		OrderView view = new OrderView();
		view.setOrderId(event.orderId());
		view.setAmount(event.amount());
		view.setStatus("CREATED");
		view.setVersion(version); // 同步初始版本

		// 防禦性設計：品項轉換
		List<OrderItemView> itemViews = Optional.ofNullable(event.items()).orElse(Collections.emptyList()).stream()
				.map(item -> new OrderItemView(item.productId(), item.quantity(), item.price()))
				.collect(Collectors.toList());

		view.setItems(itemViews);

		repository.save(view);
	}

	/**
	 * 處理訂單取消事件 (OrderCancelledEvent)
	 */
	@EventHandler
	public void on(OrderCancelledEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 訂單 {} 已取消，同步新版本: {}", event.orderId(), version);

		repository.findById(event.orderId()).ifPresent(view -> {
			view.markCancelled();
			view.setVersion(version); // 更新版本號
			repository.save(view);
		});
	}

	/**
	 * 處理通知出貨事件 (OrderNotifiedEvent)
	 */
	@EventHandler
	public void on(OrderNotifiedEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 訂單 {} 已通知出貨，同步新版本: {}", event.orderId(), version);

		repository.findById(event.orderId()).ifPresent(view -> {
			view.markNotified();
			view.setVersion(version); // 更新版本號
			repository.save(view);
		});
	}

	/**
	 * 處理訂單出貨完成事件 (OrderShippedEvent)
	 */
	@EventHandler
	public void on(OrderShippedEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 訂單 {} 出貨完成，同步新版本: {}", event.orderId(), version);

		repository.findById(event.orderId()).ifPresent(view -> {
			view.markShipped();
			view.setVersion(version); // 更新版本號
			repository.save(view);
		});
	}

	/**
	 * 處理訂單退貨事件 (OrderReturnedEvent)
	 * <p>
	 * 新增：因應退貨 API 擴充，同步更新讀取端狀態與版本。
	 * </p>
	 */
	@EventHandler
	public void on(OrderReturnedEvent event, @SequenceNumber Long version) {
		log.warn("[Projection] 訂單 {} 進入退貨程序，同步新版本: {}", event.orderId(), version);

		repository.findById(event.orderId()).ifPresent(view -> {
			view.setStatus("RETURNED");
			view.setVersion(version); // 更新版本號，這對後續可能的二次退貨操作很重要
			repository.save(view);
		});
	}
}