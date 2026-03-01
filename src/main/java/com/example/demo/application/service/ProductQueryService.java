package com.example.demo.application.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.product.query.FindAllProductsQuery;
import com.example.demo.application.domain.product.query.GetProductQuery;
import com.example.demo.application.shared.dto.ProductQueriedView;

import lombok.RequiredArgsConstructor;

/**
 * ProductQueryService - 產品查詢應用服務
 *
 * <p>
 * 本服務屬於 Application Layer，作為查詢端的門面 (Facade)。 負責協調並發送查詢訊息至
 * {@link org.axonframework.queryhandling.QueryBus}， 並將結果非同步地回傳給 Controller 層。
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

	/** Axon 提供的查詢閘道器，負責訊息的路由與派發 */
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
}