package com.example.demo.infra.processor;

import java.util.List;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.example.demo.application.shared.dto.OrderQueriedView;
import com.example.demo.application.shared.dto.PaymentQueriedView;
import com.example.demo.application.shared.query.FindAllOrdersQuery;
import com.example.demo.application.shared.query.GetOrderPaymentsQuery;
import com.example.demo.application.shared.query.GetOrderQuery;
import com.example.demo.infra.mapper.OrderMapper;
import com.example.demo.infra.mapper.PaymentMapper;
import com.example.demo.infra.persistence.OrderViewRepository;
import com.example.demo.infra.persistence.PaymentViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderQueryHandler - 訂單查詢處理器 *
 * <p>
 * 此類別為 CQRS 模式中的 Query Side 元件，負責處理來自 QueryGateway 的查詢請求。 透過監聽不同的 Query
 * 訊息，從唯讀資料庫 (Read-only Database) 中檢索資料並回傳。
 * </p>
 * *
 * <h3>設計細節：</h3>
 * <ul>
 * <li><b>DTO 映射：</b> 為了確保序列化安全，本處理器會在回傳前利用 Mapper 將 JPA Entity 轉為純粹的 DTO
 * (OrderQueriedView)。這能有效避免 Hibernate 延遲加載 (Lazy Loading) 導致的序列化異常。</li>
 * <li><b>解耦：</b> 查詢端與領域層 (Aggregate) 完全隔離，確保讀取邏輯不會干擾業務決策。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHandler {

	private final OrderViewRepository orderRepository;
	private final PaymentViewRepository paymentRepository;
	private final OrderMapper orderMapper; // 負責訂單實體與 DTO 之間的轉換
	private final PaymentMapper paymentMapper; // 負責支付實體與 DTO 之間的轉換

	/**
	 * 處理「查詢所有訂單」請求
	 * 
	 * @param query 查詢所有訂單的意圖訊息 (FindAllOrdersQuery)
	 * @return 包含所有訂單資訊的 DTO 清單
	 */
	@QueryHandler
	public List<OrderQueriedView> handle(FindAllOrdersQuery query) {
		log.info("[Query] 收到 FindAllOrdersQuery，開始檢索所有訂單資料。");
		// 從資料庫撈取所有實體，並逐一轉換為 DTO 回傳
		return orderRepository.findAll().stream().map(orderMapper::transformProjection) // 關鍵：在此層級即完成 DTO 轉換
				.toList();
	}

	/**
	 * 處理「根據 ID 查詢特定訂單」請求
	 * 
	 * @param query 包含目標 OrderId 的查詢訊息 (GetOrderQuery)
	 * @return 該訂單的 DTO 結構；若找不到則回傳 null (QueryGateway 會收到 Empty Result)
	 */
	@QueryHandler
	public OrderQueriedView handle(GetOrderQuery query) {
		log.info("[Query] 收到 GetOrderQuery，OrderId: {}", query.orderId());
		// 透過 Optional 處理查詢結果，並轉換為 DTO
		return orderRepository.findById(query.orderId()).map(orderMapper::transformProjection).orElse(null);
	}

	/**
	 * 處理「查詢訂單相關支付紀錄」請求
	 * 
	 * @param query 包含目標 OrderId 的支付查詢訊息 (GetOrderPaymentsQuery)
	 * @return 與該訂單關聯的所有支付 DTO 清單
	 */
	@QueryHandler
	public List<PaymentQueriedView> handle(GetOrderPaymentsQuery query) {
		log.info("[Query] 收到 GetOrderPaymentsQuery，OrderId: {}", query.orderId());
		// 根據 OrderId 篩選支付紀錄，並轉換為 DTO 回傳給前端
		return paymentRepository.findByOrderId(query.orderId()).stream().map(paymentMapper::transformProjection)
				.toList();
	}
}