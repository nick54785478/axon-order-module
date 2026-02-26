package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.example.demo.application.domain.order.projection.OrderView;
import com.example.demo.application.shared.dto.OrderQueriedView;
import com.example.demo.config.config.MapStructConfiguration;

@Mapper(componentModel = "spring", config = MapStructConfiguration.class)
public interface OrderMapper {

	OrderQueriedView transformProjection(OrderView projection);

	List<OrderQueriedView> transformProjection(List<OrderView> projections);
	
}
