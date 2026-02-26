package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.example.demo.application.domain.payment.projection.PaymentView;
import com.example.demo.application.shared.dto.PaymentQueriedView;
import com.example.demo.config.config.MapStructConfiguration;

@Mapper(componentModel = "spring", config = MapStructConfiguration.class)
public interface PaymentMapper {

	PaymentQueriedView transformProjection(PaymentView projection);
	
	List<PaymentQueriedView> transformProjection(List<PaymentView> projections);

}
