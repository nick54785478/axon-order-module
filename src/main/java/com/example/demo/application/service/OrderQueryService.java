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

@Service
@RequiredArgsConstructor
public class OrderQueryService {

	private final QueryGateway queryGateway;

	public CompletableFuture<List<OrderQueriedView>> getAllOrders(FindAllOrdersQuery query) {
		// 修改為 ResponseTypes.multipleInstancesOf(OrderQueriedView.class)
		return queryGateway.query(query, ResponseTypes.multipleInstancesOf(OrderQueriedView.class));
	}

	public CompletableFuture<OrderQueriedView> getOrder(GetOrderQuery query) {
		// 修改為 ResponseTypes.instanceOf(OrderQueriedView.class)
		return queryGateway.query(query, ResponseTypes.instanceOf(OrderQueriedView.class));
	}

	public CompletableFuture<List<PaymentQueriedView>> getPayments(GetOrderPaymentsQuery query) {
		return queryGateway.query(query, ResponseTypes.multipleInstancesOf(PaymentQueriedView.class));
	}
}