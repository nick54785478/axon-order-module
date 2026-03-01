package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.demo.application.domain.product.command.CreateProductCommand;
import com.example.demo.application.domain.product.projection.ProductView;
import com.example.demo.application.shared.dto.ProductQueriedView;
import com.example.demo.iface.dto.req.CreateProductResource;

/**
 * ProductMapper - 產品模組物件映射器
 * <p>
 * 負責產品實體視圖的呈現以及產品建立指令的預處理。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

	// ##### 查詢端 (Query Side) #####

	/**
	 * 將產品資料庫實體轉換為查詢 DTO (含版本號)
	 * 
	 * @param view 產品投影實體
	 * @return 包含庫存與版本資訊的 DTO
	 */
	ProductQueriedView toQueriedView(ProductView view);

	/**
	 * 將產品實體清單轉換為 DTO 清單
	 */
	List<ProductQueriedView> transformProjectionList(List<ProductView> views);

	// ##### 指令端 (Command Side) #####

	/**
	 * ACL 轉換：將 API 產品請求轉換為「建立產品」指令
	 * 
	 * @param resource 產品建立請求
	 * @return 產品建立指令
	 */
	@Mapping(target = "productId", ignore = true) // ID 由 Aggregate 自動生成
	CreateProductCommand toCommand(CreateProductResource resource);
}