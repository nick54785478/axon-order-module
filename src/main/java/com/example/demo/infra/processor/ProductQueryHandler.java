package com.example.demo.infra.processor;

import java.util.List;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.product.projection.ProductPageQueriedView;
import com.example.demo.application.domain.product.projection.ProductView;
import com.example.demo.application.domain.product.query.FindAllProductsQuery;
import com.example.demo.application.domain.product.query.FindProductsPagedQuery;
import com.example.demo.application.domain.product.query.GetProductQuery;
import com.example.demo.application.shared.dto.ProductQueriedView;
import com.example.demo.infra.mapper.ProductMapper;
import com.example.demo.infra.persistence.ProductViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProductQueryHandler - 產品查詢處理器
 *
 * <p>
 * 此類別屬於 Infrastructure Layer (Driven Adapter)，負責實作產品相關的查詢邏輯。 透過監聽 Query Bus
 * 上的訊息，從 MySQL 讀取模型 (ProductView) 中檢索資料。
 * </p>
 *
 * <h3>設計細節：</h3>
 * <ul>
 * <li><b>DTO 轉換：</b> 延續我們在訂單模組的優化策略，在此層級直接將 JPA Entity 轉換為 DTO， 確保跨網絡傳輸時不會觸發
 * Hibernate 的延遲加載異常。</li>
 * <li><b>非同步支持：</b> 配合 QueryGateway，提供高效的唯讀資料存取。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductQueryHandler {

	private final ProductViewRepository repository;
	private final ProductMapper mapper;

	/**
	 * 處理「查詢所有產品」請求
	 *
	 * @param query 查詢所有產品的意圖訊息 (FindAllProductsQuery)
	 * @return 轉換後的產品 DTO 清單
	 */
	@QueryHandler
	public List<ProductQueriedView> handle(FindAllProductsQuery query) {
		log.info("[Query] 處理 FindAllProductsQuery，檢索產品清單。");

		return repository.findAll().stream().map(mapper::toQueriedView) // 在此處進行 DTO 映射
				.toList();
	}

	/**
	 * 處理「根據 ID 查詢特定產品」請求
	 *
	 * @param query 包含產品 ID 的查詢訊息 (GetProductQuery)
	 * @return 產品 DTO；若不存在則回傳 null
	 */
	@QueryHandler
	public ProductQueriedView handle(GetProductQuery query) {
		log.info("[Query] 處理 GetProductQuery，查詢 ID: {}", query.productId());

		return repository.findById(query.productId()).map(mapper::toQueriedView).orElse(null);
	}

	/**
	 * 處理產品分頁查詢
	 * <p>
	 * 將過濾條件從 Query 訊息中提取，並透過 Repository 執行物理分頁。
	 * </p>
	 */
	@QueryHandler
	public ProductPageQueriedView handle(FindProductsPagedQuery query) {
		log.info("[Query] 收到分頁查詢請求：名稱={}, 頁碼={}", query.name(), query.page());

		Pageable pageable = PageRequest.of(query.page(), query.size());

		Page<ProductView> resultPage = (query.name() != null && !query.name().isBlank())
				? repository.findByNameContainingIgnoreCase(query.name(), pageable)
				: repository.findAll(pageable);

		List<ProductQueriedView> content = resultPage.getContent().stream().map(mapper::toQueriedView).toList();

		return new ProductPageQueriedView(content, resultPage.getTotalElements(), resultPage.getTotalPages(),
				resultPage.getNumber());
	}
}