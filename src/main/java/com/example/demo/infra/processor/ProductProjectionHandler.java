package com.example.demo.infra.processor;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.product.event.ProductCreatedEvent;
import com.example.demo.application.domain.product.event.StockReducedEvent;
import com.example.demo.application.domain.product.projection.ProductView;
import com.example.demo.application.shared.event.StockAddedEvent;
import com.example.demo.infra.persistence.ProductViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProductProjectionHandler - 產品投影處理器
 * <p>
 * 監聽產品領域事件並將其同步至 MySQL 讀取模型。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("product-group")
public class ProductProjectionHandler {

	private final ProductViewRepository repository;

	/**
	 * 處理產品建立事件
	 */
	@EventHandler
	public void on(ProductCreatedEvent event) {
		ProductView view = new ProductView();
		view.setProductId(event.productId());
		view.markCreated(event.name(), event.price(), event.initialStock());
		repository.save(view);
	}

	/**
	 * 處理庫存扣減事件
	 */
	@EventHandler
	public void on(StockReducedEvent event) {
		repository.findById(event.productId()).ifPresent(view -> {
			view.reduceStock(event.quantity());
			repository.save(view);
		});
	}

	/**
	 * 處理庫存還原事件
	 */
	@EventHandler
	public void on(StockAddedEvent event) {
		log.info("[Projection] 還原產品庫存: {}", event.productId());
		repository.findById(event.productId()).ifPresent(view -> {
			// 在 ProductView 中新增對應的增加邏輯
			int currentStock = view.getStock() != null ? view.getStock() : 0;
			view.setStock(currentStock + event.quantity());
			repository.save(view);
		});
	}
}