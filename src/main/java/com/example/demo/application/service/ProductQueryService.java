package com.example.demo.application.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import com.example.demo.application.shared.dto.ProductQueriedView;
import com.example.demo.application.shared.query.FindAllProductsQuery;
import com.example.demo.application.shared.query.GetProductQuery;

import lombok.RequiredArgsConstructor;

/**
 * ProductQueryService - 產品查詢應用服務
 */
@Service
@RequiredArgsConstructor
public class ProductQueryService {

	private final QueryGateway queryGateway;

	public CompletableFuture<List<ProductQueriedView>> findAll() {
		return queryGateway.query(new FindAllProductsQuery(),
				ResponseTypes.multipleInstancesOf(ProductQueriedView.class));
	}

	public CompletableFuture<ProductQueriedView> findById(String productId) {
		return queryGateway.query(new GetProductQuery(productId), ResponseTypes.instanceOf(ProductQueriedView.class));
	}
}