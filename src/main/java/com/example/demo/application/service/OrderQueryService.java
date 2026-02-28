package com.example.demo.application.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.shared.dto.OrderQueriedView;
import com.example.demo.application.shared.dto.PaymentQueriedView;
import com.example.demo.application.shared.query.FindAllOrdersQuery;
import com.example.demo.application.shared.query.GetOrderPaymentsQuery;
import com.example.demo.application.shared.query.GetOrderQuery;

import lombok.RequiredArgsConstructor;

/**
 * OrderQueryService - 訂單查詢應用服務
 *
 * <p>
 * 本服務屬於 Application Layer，作為查詢端的門面 (Facade)。 負責透過 Axon 的 {@link QueryGateway}
 * 將查詢意圖 (Query Message) 分派至對應的處理器。
 * </p>
 *
 * <h3>設計重點：</h3>
 * <ul>
 * <li><b>非同步響應：</b> 所有方法皆回傳 {@link CompletableFuture}，支援非阻塞的 API 調用。</li>
 * <li><b>類型安全：</b> 透過 {@link ResponseTypes} 明確定義預期的回傳資料格式（DTO），確保傳輸的一致性。</li>
 * <li><b>職責分離：</b> 本服務不涉及任何資料庫存取與轉換邏輯，僅負責訊息的調度。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

	/**
	 * Axon 框架提供的查詢發送閘道器，負責將 Query 路由至對應的 @QueryHandler
	 */
	private final QueryGateway queryGateway;

	/**
	 * 獲取系統中所有訂單的清單
	 *
	 * <p>
	 * 發送 {@link FindAllOrdersQuery} 並期待回傳多個 {@link OrderQueriedView} 實例。
	 * </p>
	 *
	 * @param query 查詢所有訂單的意圖訊息
	 * @return 包含訂單 DTO 列表的非同步結果
	 */
	public CompletableFuture<List<OrderQueriedView>> getAllOrders(FindAllOrdersQuery query) {
		// 使用 multipleInstancesOf 告訴 Axon 預期回傳的是一個集合
		return queryGateway.query(query, ResponseTypes.multipleInstancesOf(OrderQueriedView.class));
	}

	/**
	 * 獲取特定訂單的詳細資訊
	 *
	 * <p>
	 * 根據訂單 ID 檢索單一紀錄，包含品項明細與金額狀態。
	 * </p>
	 *
	 * @param query 包含目標 OrderId 的查詢訊息
	 * @return 包含單一訂單 DTO 的非同步結果
	 */
	public CompletableFuture<OrderQueriedView> getOrder(GetOrderQuery query) {
		// 使用 instanceOf 期待回傳單一物件實例
		return queryGateway.query(query, ResponseTypes.instanceOf(OrderQueriedView.class));
	}

	/**
	 * 獲取特定訂單下屬的所有支付紀錄
	 *
	 * <p>
	 * 常用於訂單詳情頁面的支付歷史展示，或是 Saga 補償流程後的狀態核對。
	 * </p>
	 *
	 * @param query 包含目標 OrderId 的支付查詢訊息
	 * @return 包含支付紀錄 DTO 列表的非同步結果
	 */
	public CompletableFuture<List<PaymentQueriedView>> getPayments(GetOrderPaymentsQuery query) {
		// 透過 QueryGateway 尋找專門處理支付視圖的 Handler
		return queryGateway.query(query, ResponseTypes.multipleInstancesOf(PaymentQueriedView.class));
	}
}