package com.example.demo.infra.processor;

import java.util.List;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.order.query.FindAllOrdersQuery;
import com.example.demo.application.domain.order.query.GetOrderQuery;
import com.example.demo.application.shared.dto.OrderQueriedView;
import com.example.demo.application.shared.dto.PaymentQueriedView;
import com.example.demo.application.shared.query.GetOrderPaymentsQuery;
import com.example.demo.infra.mapper.OrderMapper;
import com.example.demo.infra.mapper.PaymentMapper;
import com.example.demo.infra.persistence.OrderViewRepository;
import com.example.demo.infra.persistence.PaymentViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderQueryHandler - 訂單與支付查詢處理器
 *
 * <p>
 * 本類別屬於 Infrastructure Layer (Driven Adapter)，作為查詢端的訊息處理器。 負責監聽
 * {@link org.axonframework.queryhandling.QueryBus} 上的訊息，並從唯讀資料庫中檢索資料。
 * </p>
 *
 * <h3>技術要點：</h3>
 * <ul>
 * <li><b>Lazy Loading 防禦：</b> 在此處理器內立即進行 DTO 映射 (Mapping)，防止 Hibernate Proxy
 * 物件流向外層，從根源解決序列化失敗與 {@code LazyInitializationException}。</li>
 * <li><b>最終一致性：</b> 查詢端反映的是 {@code ProjectionHandler} 同步後的狀態快照，不保證與 Command
 * Side 絕對實時同步。</li>
 * <li><b>多模型查詢：</b> 同時處理訂單與關聯支付的檢索，提供前端完整的事務視圖。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHandler {

	/**
	 * 訂單唯讀資料庫操作介面
	 */
	private final OrderViewRepository orderRepository;

	/**
	 * 支付唯讀資料庫操作介面
	 */
	private final PaymentViewRepository paymentRepository;

	/**
	 * MapStruct 映射器：負責將 JPA Entity 轉換為包含品項明細的 DTO
	 */
	private final OrderMapper orderMapper;

	/**
	 * MapStruct 映射器：負責將支付實體轉換為 API 專用 DTO
	 */
	private final PaymentMapper paymentMapper;

	/**
	 * 處理「查詢所有訂單」請求
	 * <p>
	 * 檢索系統內所有訂單，並將其轉化為摘要/清單格式。
	 * </p>
	 *
	 * @param query 查詢所有訂單的意圖訊息 (FindAllOrdersQuery)
	 * @return 包含訂單與品項詳情的 DTO 清單
	 */
	@QueryHandler
	public List<OrderQueriedView> handle(FindAllOrdersQuery query) {
		log.info("[Query] 處理 FindAllOrdersQuery，執行全量訂單檢索。");

		return orderRepository.findAll().stream().map(orderMapper::toQueriedView) // 轉換為包含 OrderItemQueriedView 的 DTO
				.toList();
	}

	/**
	 * 處理「根據 ID 查詢特定訂單」請求
	 * <p>
	 * 提供單一訂單的完整詳情，常用於訂單確認頁面或後台明細檢索。
	 * </p>
	 *
	 * @param query 包含目標 OrderId 的查詢訊息 (GetOrderQuery)
	 * @return 目標訂單的 DTO 結構；若 ID 不存在則回傳 null。
	 */
	@QueryHandler
	public OrderQueriedView handle(GetOrderQuery query) {
		log.info("[Query] 處理 GetOrderQuery，OrderId: {}", query.orderId());

		return orderRepository.findById(query.orderId()).map(orderMapper::toQueriedView).orElse(null);
	}

	/**
	 * 處理「查詢訂單相關支付紀錄」請求
	 * <p>
	 * 在 Saga 補償流程或金流核對時，用於獲取該訂單的所有支付嘗試紀錄。
	 * </p>
	 *
	 * @param query 包含目標 OrderId 的支付查詢訊息 (GetOrderPaymentsQuery)
	 * @return 與該訂單關聯的支付紀錄 DTO 清單 (可能包含 CREATED, PROCESSED, REFUNDED 等多筆)
	 */
	@QueryHandler
	public List<PaymentQueriedView> handle(GetOrderPaymentsQuery query) {
		log.info("[Query] 處理 GetOrderPaymentsQuery，目標訂單: {}", query.orderId());

		// 透過訂單 ID 過濾關聯的支付快照
		return paymentRepository.findByOrderId(query.orderId()).stream().map(paymentMapper::toQueriedView).toList();
	}
}