package com.example.demo.application.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.product.projection.ProductPageQueriedView;
import com.example.demo.application.domain.product.query.FindAllProductsQuery;
import com.example.demo.application.domain.product.query.FindProductsPagedQuery;
import com.example.demo.application.domain.product.query.GetProductQuery;
import com.example.demo.application.shared.dto.ProductQueriedView;

import lombok.RequiredArgsConstructor;

/**
 * ProductQueryService - 產品查詢應用服務
 *
 * <p>
 * 本服務屬於 Application Layer，作為查詢端的門面 (Facade)。
 * </p>
 * <p>
 * 負責協調並發送查詢訊息至 {@link org.axonframework.queryhandling.QueryBus}， 並將結果非同步地回傳給
 * Interface Layer。
 * </p>
 *
 * <h3>設計核心：</h3>
 * <ul>
 * <li><b>解耦查詢邏輯：</b> 透過 QueryGateway，本服務不需要知道資料具體儲存在何處 (MySQL, Mongo 等)。</li>
 * <li><b>非阻塞響應：</b> 所有方法皆回傳 {@link CompletableFuture}，提升高併發環境下的系統吞吐量。</li>
 * <li><b>類型安全：</b> 利用 Axon 的 ResponseTypes 確保回傳資料與 DTO 結構一致。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProductQueryService {

	/**
	 * Axon 提供的查詢閘道器，負責訊息的路由與派發
	 */
	private final QueryGateway queryGateway;

	/**
	 * 檢索系統中所有產品的清單
	 *
	 * <p>
	 * 發送 {@link FindAllProductsQuery}，並期待回傳一組產品投影 DTO。
	 * </p>
	 *
	 * @return 包含 {@link ProductQueriedView} 列表的非同步結果
	 */
	public CompletableFuture<List<ProductQueriedView>> findAll() {
		// 使用 multipleInstancesOf 告訴 Axon 預期回傳的是一個集合
		return queryGateway.query(new FindAllProductsQuery(),
				ResponseTypes.multipleInstancesOf(ProductQueriedView.class));
	}

	/**
	 * 根據產品識別碼獲取特定產品詳情
	 *
	 * <p>
	 * 常用於產品詳情頁面或在執行指令前的資料校驗。
	 * </p>
	 *
	 * @param productId 產品唯一識別碼
	 * @return 包含單一 {@link ProductQueriedView} 的非同步結果；若找不到則回傳 Optional 或 null
	 */
	public CompletableFuture<ProductQueriedView> findById(String productId) {
		// 使用 instanceOf 期待回傳單一物件實例
		return queryGateway.query(new GetProductQuery(productId), ResponseTypes.instanceOf(ProductQueriedView.class));
	}

	/**
	 * 產品分頁查詢服務 (後端分頁與過濾) *
	 * <p>
	 * 透過發送 {@link FindProductsPagedQuery} 進行資料檢索。 為了確保在分散式環境下（如使用 Axon
	 * Server）訊息路由的精確性， 本方法明確要求回傳具體類別 {@link ProductPageQueriedView}，以規避 Java
	 * 泛型擦除導致的匹配失敗。
	 * </p>
	 *
	 * @param name 產品名稱關鍵字（支援模糊查詢）；若為空則檢索全量產品
	 * @param page 目標頁碼 (從 0 開始，0 代表第一頁)
	 * @param size 每頁顯示的資料筆數上限
	 * @return 包含分頁內容、總筆數、總頁數等中繼資料的非同步結果
	 */
	public CompletableFuture<ProductPageQueriedView> findPaged(String name, int page, int size) {
		return queryGateway.query(new FindProductsPagedQuery(name, page, size),
				// 關鍵：使用 ResponseTypes.instanceOf(具體類別) 以解決 NoHandlerForQueryException
				ResponseTypes.instanceOf(ProductPageQueriedView.class));
	}
}