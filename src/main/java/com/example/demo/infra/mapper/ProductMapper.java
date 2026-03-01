package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.demo.application.domain.product.command.CreateProductCommand;
import com.example.demo.application.domain.product.projection.ProductView;
import com.example.demo.application.shared.dto.ProductQueriedView;
import com.example.demo.iface.dto.req.CreateProductResource;

@Mapper(componentModel = "spring")
public interface ProductMapper {

	/**
	 * 將資料庫實體轉換為 DTO
	 */
	ProductQueriedView transformProjection(ProductView view);

	/**
	 * 將實體列表轉換為 DTO 列表
	 */
	List<ProductQueriedView> transformProjectionList(List<ProductView> views);

	/**
	 * 將 API 請求轉換為建立指令 (ACL 防腐層)
	 */
	@Mapping(target = "productId", ignore = true) // 讓指令內部的建構子自動生成 ID
	CreateProductCommand toCommand(CreateProductResource resource);
}