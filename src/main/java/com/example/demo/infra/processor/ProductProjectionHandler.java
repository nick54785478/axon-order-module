package com.example.demo.infra.processor;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.SequenceNumber;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.product.event.ProductCreatedEvent;
import com.example.demo.application.domain.product.event.ProductUpdatedEvent;
import com.example.demo.application.domain.product.event.StockReducedEvent;
import com.example.demo.application.domain.product.projection.ProductView;
import com.example.demo.application.shared.event.StockAddedEvent;
import com.example.demo.infra.persistence.ProductViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup("product-group")
public class ProductProjectionHandler {

	private final ProductViewRepository repository;

	/**
	 * 處理產品建立事件 (Version 0)
	 */
	@EventHandler
	public void on(ProductCreatedEvent event, @SequenceNumber Long version) {
		ProductView view = new ProductView();
		view.setProductId(event.productId());
		view.markCreated(event.name(), event.price(), event.initialStock(), version);
		repository.save(view);
	}

	/**
	 * 處理產品更新事件
	 */
	@EventHandler
	public void on(ProductUpdatedEvent event, @SequenceNumber Long version) {
		repository.findById(event.productId()).ifPresent(view -> {
			view.updateInfo(event.name(), event.price(), version);
			repository.save(view);
		});
	}

	/**
	 * 處理庫存扣減事件
	 */
	@EventHandler
	public void on(StockReducedEvent event, @SequenceNumber Long version) {
		repository.findById(event.productId()).ifPresent(view -> {
			// 注意：這裡建議使用 view 內部的業務方法更新，保持封裝
			int newStock = view.getStock() - event.quantity();
			view.updateStock(newStock, version);
			repository.save(view);
		});
	}

	/**
	 * 處理庫存還原事件 (也要更新版本號！) * @param version 來自聚合根的最新序列號
	 */
	@EventHandler
	public void on(StockAddedEvent event, @SequenceNumber Long version) {
		log.info("[Projection] 還原產品庫存: {}，最新版本: {}", event.productId(), version);
		repository.findById(event.productId()).ifPresent(view -> {
			int currentStock = view.getStock() != null ? view.getStock() : 0;
			// 確保版本號同步，防止後續手動更新時發生樂觀鎖衝突
			view.updateStock(currentStock + event.quantity(), version);
			repository.save(view);
		});
	}
}