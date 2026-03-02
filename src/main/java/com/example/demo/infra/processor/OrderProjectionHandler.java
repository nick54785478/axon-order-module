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
 *
 * <p>
 * 此類別屬於六角形架構中的「驅動適配器 (Driven Adapter)」，負責實作 CQRS 模式中的讀取端 (Query Side) 邏輯。
 * 透過監聽訂單領域事件 (Domain Events)，將寫入端 (Aggregate) 的狀態變更即時同步至 MySQL 資料庫的
 * {@link OrderView} 中。
 * </p>
 *
 * <h3>核心升級：版本同步與樂觀鎖防護</h3>
 * <ul>
 * <li><b>版本映射：</b> 透過 {@code @SequenceNumber} 獲取事件在 Event Store 中的全局序列號。</li>
 * <li><b>一致性保證：</b> 將序列號存儲為 {@code OrderView} 的
 * version，確保前端查詢到的版本號能精確對應領域模型的最新狀態。</li>
 * <li><b>前端賦能：</b> 支援前端 API 呼叫時傳回版本號，從而觸發寫入端的樂觀鎖 (Optimistic Locking) 校驗。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("order-group") // 指定處理群組，用於管理 Tracking Token 與並行處理策略
public class OrderProjectionHandler {

	private final OrderViewRepository repository;

	/**
	 * 處理訂單建立事件 (OrderCreatedEvent)
	 * <p>
	 * 當新訂單在 Aggregate 中成功建立後觸發。此為投影模型的初始化階段。
	 * </p>
	 * 
	 * @param event   訂單建立事實，包含品項、金額等基礎資訊
	 * @param version 來自 Axon Event Store 的序列號 (初始通常為 0)
	 */
	@EventHandler
	public void on(OrderCreatedEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 接收到訂單建立事件: {}, 版本同步: {}", event.orderId(), version);

		OrderView view = new OrderView();
		view.setOrderId(event.orderId());
		view.setAmount(event.amount());
		view.setStatus("CREATED");
		view.setVersion(version); // 💡 重要：同步寫入端的初始版本

		// --- 數據轉換與防禦性設計 ---
		// 確保品項清單不為 null，將領域品項轉換為視圖實體
		List<OrderItemView> itemViews = Optional.ofNullable(event.items()).orElse(Collections.emptyList()).stream()
				.map(item -> new OrderItemView(item.productId(), item.quantity(), item.price())).toList();

		view.setItems(itemViews);

		repository.save(view);
		log.debug("[Projection] OrderView 初始化完成: {}", event.orderId());
	}

	/**
	 * 處理訂單取消事件 (OrderCancelledEvent)
	 * <p>
	 * 同步更新讀取端狀態為「CANCELLED」。此操作可能由用戶取消、庫存失敗或支付超時觸發。
	 * </p>
	 * 
	 * @param event   訂單取消事件
	 * @param version 該操作對應的事件序列號
	 */
	@EventHandler
	public void on(OrderCancelledEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 更新訂單取消狀態: {}, 新版本: {}", event.orderId(), version);

		repository.findById(event.orderId()).ifPresentOrElse(view -> {
			view.markCancelled(); // 執行充血模型內部的狀態變更
			view.setVersion(version); // 更新版本以反映最新狀態
			repository.save(view);
		}, () -> log.warn("[Projection] 找不到欲更新的訂單(取消): {}", event.orderId()));
	}

	/**
	 * 處理通知出貨事件 (OrderNotifiedEvent)
	 * <p>
	 * 當支付模組成功處理扣款後，由 Saga 觸發。此狀態代表訂單已進入可出貨階段。
	 * </p>
	 * 
	 * @param event   通知出貨事件
	 * @param version 該操作對應的事件序列號
	 */
	@EventHandler
	public void on(OrderNotifiedEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 訂單已支付並通知出貨: {}, 新版本: {}", event.orderId(), version);

		repository.findById(event.orderId()).ifPresent(view -> {
			view.markNotified();
			view.setVersion(version);
			repository.save(view);
		});
	}

	/**
	 * 處理訂單出貨完成事件 (OrderShippedEvent)
	 * <p>
	 * 同步手動確認出貨的結果。出貨後，讀取端將禁止進行「取消」操作。
	 * </p>
	 * 
	 * @param event   出貨完成事件
	 * @param version 該操作對應的事件序列號
	 */
	@EventHandler
	public void on(OrderShippedEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 訂單出貨狀態同步: {}, 新版本: {}", event.orderId(), version);

		repository.findById(event.orderId()).ifPresent(view -> {
			view.markShipped();
			view.setVersion(version);
			repository.save(view);
		});
	}

	/**
	 * 處理訂單退貨事件 (OrderReturnedEvent)
	 * <p>
	 * 監聽逆向物流流程。當訂單狀態轉為「RETURNED」時，同步更新視圖。 更新版本號後，前端可根據此版本進行後續的二次申訴或歸檔操作。
	 * </p>
	 * * @param event 退貨事實，包含退貨原因
	 * 
	 * @param version 該操作對應的事件序列號
	 */
	@EventHandler
	public void on(OrderReturnedEvent event, @SequenceNumber Long version) {
		log.warn("[Projection] 訂單退貨狀態同步: {}, 原因: {}, 新版本: {}", event.orderId(), event.reason(), version);

		repository.findById(event.orderId()).ifPresent(view -> {
			// 直接調用 Setter 或實體方法變更狀態
			view.setStatus("RETURNED");
			view.setVersion(version);
			repository.save(view);
		});
	}
}