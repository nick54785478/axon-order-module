package com.example.demo.infra.processor;

import java.util.List;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.product.query.FindAllProductsQuery;
import com.example.demo.application.domain.product.query.FindProductsPagedQuery;
import com.example.demo.application.domain.product.query.GetProductQuery;
import com.example.demo.application.shared.dto.ProductPageQueriedView;
import com.example.demo.application.shared.dto.ProductQueriedView;
import com.example.demo.infra.mapper.ProductMapper;
import com.example.demo.infra.persistence.ProductViewRepository;
import com.example.demo.infra.projection.product.ProductView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProductQueryHandler - 產品查詢處理器
 *
 * <p>
 * 此類別作為 CQRS 架構中的查詢端適配器（Query Adapter），負責處理路由在 Query Bus 上的查詢請求。 透過串接
 * {@link ProductViewRepository}，從物理資料庫檢索投影後的產品數據（ProductView）。
 * </p>
 *
 * <h3>核心特點：</h3>
 * <ul>
 * <li><b>領域隔離：</b> 嚴格執行實體（Entity）與 DTO（QueriedView）的轉換，防止讀取模型變更影響外部介面。</li>
 * <li><b>效能優化：</b> 採用 {@link Pageable} 實作資料庫層級的分頁，避免大數據量下記憶體溢出的風險。</li>
 * <li><b>解耦查詢：</b> 透過 Axon Query Bus 實現 API 與查詢邏輯的非同步解耦。</li>
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
	 * <p>
	 * 用於後台管理或全量產品同步場景。將資料庫中所有的產品視圖實體批次轉換為 DTO。
	 * </p>
	 *
	 * @param query 查詢所有產品的意圖訊息 (FindAllProductsQuery)
	 * @return 轉換後的產品 DTO 清單 (List of ProductQueriedView)
	 */
	@QueryHandler
	public List<ProductQueriedView> handle(FindAllProductsQuery query) {
		log.info("[Query] 處理 FindAllProductsQuery，執行全量產品檢索。");

		// 透過 Stream API 進行平滑的 DTO 映射轉換
		return repository.findAll().stream().map(mapper::toQueriedView).toList();
	}

	/**
	 * 處理「根據 ID 查詢特定產品」請求
	 * <p>
	 * 供產品詳情頁使用。若找不到對應產品，則回傳 null（Axon 會自動封裝為 Optional 的空值）。
	 * </p>
	 *
	 * @param query 包含產品唯一識別碼的查詢訊息 (GetProductQuery)
	 * @return 產品 DTO；若不存在則回傳 null
	 */
	@QueryHandler
	public ProductQueriedView handle(GetProductQuery query) {
		log.info("[Query] 處理 GetProductQuery，查詢目標 ID: {}", query.productId());

		return repository.findById(query.productId()).map(mapper::toQueriedView).orElse(null);
	}

	/**
	 * 處理產品分頁與過濾查詢
	 * <p>
	 * 實作動態查詢邏輯： 1. 若提供 {@code name} 參數，則執行忽略大小寫的模糊查詢。 2. 結合 {@link PageRequest}
	 * 將分頁參數（Page, Size）下壓至資料庫 SQL 層執行。
	 * </p>
	 * * @param query 包含篩選條件與分頁參數的查詢訊息 (FindProductsPagedQuery)
	 * 
	 * @return 包含分頁元數據（總頁數、總筆數）的包裝 DTO (ProductPageQueriedView)
	 */
	@QueryHandler
	public ProductPageQueriedView handle(FindProductsPagedQuery query) {
		log.info("[Query] 收到分頁查詢請求：名稱過濾={}, 目前頁碼={}, 每頁筆數={}", query.name(), query.page(), query.size());

		// 封裝 Spring Data JPA 的分頁物件
		Pageable pageable = PageRequest.of(query.page(), query.size());

		// 根據條件執行物理分頁查詢
		Page<ProductView> resultPage = (query.name() != null && !query.name().isBlank())
				? repository.findByNameContainingIgnoreCase(query.name(), pageable)
				: repository.findAll(pageable);

		// 將結果內容轉換為 DTO 並包裝分頁統計資訊
		List<ProductQueriedView> content = resultPage.getContent().stream().map(mapper::toQueriedView).toList();

		log.debug("[Query] 分頁查詢完成，總筆數: {}, 總頁數: {}", resultPage.getTotalElements(), resultPage.getTotalPages());

		return new ProductPageQueriedView(content, resultPage.getTotalElements(), resultPage.getTotalPages(),
				resultPage.getNumber());
	}
}