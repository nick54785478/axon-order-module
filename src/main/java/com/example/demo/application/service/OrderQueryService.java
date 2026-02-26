package com.example.demo.application.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.domain.order.projection.OrderView;
import com.example.demo.application.domain.payment.projection.PaymentView;
import com.example.demo.application.shared.dto.OrderQueriedView;
import com.example.demo.application.shared.dto.PaymentQueriedView;
import com.example.demo.application.shared.query.FindAllOrdersQuery;
import com.example.demo.application.shared.query.GetOrderPaymentsQuery;
import com.example.demo.application.shared.query.GetOrderQuery;
import com.example.demo.infra.mapper.OrderMapper;
import com.example.demo.infra.mapper.PaymentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

	private final QueryGateway queryGateway;
	private final OrderMapper orderMapper;
	private final PaymentMapper paymentMapper;

	/**
	 * 獲取訂單清單
	 * 
	 * @param query {@link FindAllOrdersQuery}
	 * @return CompletableFuture<List<OrderQueriedView>>
	 */
	public CompletableFuture<List<OrderQueriedView>> getAllOrders(FindAllOrdersQuery query) {
		return queryGateway.query(query, ResponseTypes.multipleInstancesOf(OrderView.class))
				.thenApply(orderMapper::transformProjection); // 轉換為 DTO List;
	}

	/**
	 * 獲取特定訂單細節
	 * 
	 * @param query {@link GetOrderQuery}
	 * @return CompletableFuture<OrderQueriedView>
	 */
	public CompletableFuture<OrderQueriedView> getOrder(GetOrderQuery query) {
		return queryGateway.query(query, ResponseTypes.instanceOf(OrderView.class))
				.thenApply(orderMapper::transformProjection);
	}

	/**
	 * 獲取特定訂單的付款狀態
	 * 
	 * @param query {@link GetOrderPaymentsQuery}
	 * @return CompletableFuture<List<PaymentQueriedView>>
	 */
	public CompletableFuture<List<PaymentQueriedView>> getPayments(GetOrderPaymentsQuery query) {
		return queryGateway.query(query, ResponseTypes.multipleInstancesOf(PaymentView.class))
				.thenApply(paymentMapper::transformProjection); // 轉換為 DTO List;
	}

}
