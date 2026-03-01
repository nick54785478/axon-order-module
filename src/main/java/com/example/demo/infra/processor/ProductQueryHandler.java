package com.example.demo.infra.processor;

import java.util.List;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.example.demo.application.shared.dto.ProductQueriedView;
import com.example.demo.application.shared.query.FindAllProductsQuery;
import com.example.demo.application.shared.query.GetProductQuery;
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
@Component
@RequiredArgsConstructor
@Slf4j
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

		return repository.findAll().stream().map(mapper::transformProjection) // 在此處進行 DTO 映射
				.toList(); // 使用 Java 16+ 語法
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

		return repository.findById(query.productId()).map(mapper::transformProjection).orElse(null);
	}
}