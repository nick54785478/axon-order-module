package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.demo.application.domain.order.aggregate.vo.OrderItem;
import com.example.demo.application.domain.order.command.CreateOrderCommand;
import com.example.demo.application.domain.order.projection.OrderItemView;
import com.example.demo.application.domain.order.projection.OrderView;
import com.example.demo.application.shared.dto.OrderQueriedView;
import com.example.demo.application.shared.dto.OrderQueriedView.OrderItemQueriedView;
import com.example.demo.config.config.MapStructConfiguration;
import com.example.demo.iface.dto.req.CreateOrderItemResource;
import com.example.demo.iface.dto.req.CreateOrderResource;

@Mapper(componentModel = "spring", config = MapStructConfiguration.class)
public interface OrderMapper {

	OrderQueriedView transformProjection(OrderView projection);

	OrderItemQueriedView toItemDto(OrderItemView itemView);

	List<OrderQueriedView> transformProjection(List<OrderView> projections);

	@Mapping(target = "orderId", ignore = true)
	CreateOrderCommand transformACL(CreateOrderResource resource);

	OrderItem transform(CreateOrderItemResource resource);

}
