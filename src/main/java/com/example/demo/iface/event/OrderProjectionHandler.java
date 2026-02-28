package com.example.demo.iface.event;

import java.util.List;
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

@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("order-group")
public class OrderProjectionHandler {

	private final OrderViewRepository repository;

	/**
     * 當訂單建立事件發生時，同步更新 Read Model
     */
    @EventHandler
    public void on(OrderCreatedEvent event) {
        OrderView view = new OrderView();
        view.setOrderId(event.orderId());
        view.setAmount(event.amount());
        view.setStatus("CREATED");

        // 將領域物件 OrderItem 轉換為投影實體 OrderItemView
        List<OrderItemView> itemViews = event.items().stream()
            .map(item -> new OrderItemView(
                item.productId(), 
                item.quantity(), 
                item.price()
            ))
            .collect(Collectors.toList());

        view.setItems(itemViews);
        
        repository.save(view);
    }


	@EventHandler
	public void on(OrderCancelledEvent event) {
		log.info("[Projection] 更新 OrderView 為 CANCELLED: {}", event.orderId());
		repository.findById(event.orderId()).ifPresentOrElse(view -> {
			view.markCancelled();
			repository.save(view);
		}, () -> log.warn("找不到訂單: {}", event.orderId()));
	}

	@EventHandler
	public void on(OrderNotifiedEvent event) {
		repository.findById(event.orderId()).ifPresent(view -> {
			view.setStatus("NOTIFIED");
			repository.save(view);
		});
	}

	@EventHandler
	public void on(OrderShippedEvent event) {
		log.info("[Projection] 更新 OrderView 為 SHIPPED: {}", event.orderId());
		repository.findById(event.orderId()).ifPresentOrElse(view -> {
			view.markShipped();
			repository.save(view);
		}, () -> log.warn("找不到訂單: {}", event.orderId()));
	}
}