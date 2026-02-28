package com.example.demo.iface.event;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHandler {

	private final OrderViewRepository orderRepository;
	private final PaymentViewRepository paymentRepository;
	private final OrderMapper orderMapper; // 注入 Mapper
	private final PaymentMapper paymentMapper; // 注入 Mapper

	@QueryHandler
	public List<OrderQueriedView> handle(FindAllOrdersQuery query) {
		log.info("[Query] 查詢所有訂單並轉換為 DTO");
		return orderRepository.findAll().stream().map(orderMapper::transformProjection) // 在這裡執行轉換
				.toList();
	}

	@QueryHandler
	public OrderQueriedView handle(GetOrderQuery query) {
		log.info("[Query] 查詢單一訂單 DTO: {}", query.orderId());
		return orderRepository.findById(query.orderId()).map(orderMapper::transformProjection).orElse(null);
	}

	@QueryHandler
	public List<PaymentQueriedView> handle(GetOrderPaymentsQuery query) {
		log.info("[Query] 查詢訂單付款紀錄 DTO: {}", query.orderId());
		return paymentRepository.findByOrderId(query.orderId()).stream().map(paymentMapper::transformProjection)
				.toList();
	}
}