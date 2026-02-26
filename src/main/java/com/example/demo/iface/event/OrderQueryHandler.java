package com.example.demo.iface.event;

import java.util.List;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.order.projection.OrderView;
import com.example.demo.application.domain.payment.projection.PaymentView;
import com.example.demo.application.shared.query.FindAllOrdersQuery;
import com.example.demo.application.shared.query.GetOrderPaymentsQuery;
import com.example.demo.application.shared.query.GetOrderQuery;
import com.example.demo.infra.persistence.OrderViewRepository;
import com.example.demo.infra.persistence.PaymentViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHandler {

	private final OrderViewRepository orderRepository;
	private final PaymentViewRepository paymentRepository;

	/**
	 * 處理查詢所有訂單
	 */
	@QueryHandler
	public List<OrderView> handle(FindAllOrdersQuery query) {
		log.info("[Query] 查詢所有訂單");
		return orderRepository.findAll();
	}

	/**
	 * 處理根據 ID 查詢訂單
	 */
	@QueryHandler
	public OrderView handle(GetOrderQuery query) {
		log.info("[Query] 查詢單一訂單: {}", query.orderId());
		return orderRepository.findById(query.orderId()).orElse(null);
	}

	/**
	 * 處理查詢該訂單下的所有付款資訊
	 */
	@QueryHandler
	public List<PaymentView> handle(GetOrderPaymentsQuery query) {
		log.info("[Query] 查詢訂單付款紀錄: {}", query.orderId());
		return paymentRepository.findByOrderId(query.orderId());
	}
}