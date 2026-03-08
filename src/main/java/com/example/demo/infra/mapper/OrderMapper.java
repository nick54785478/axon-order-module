package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.demo.application.domain.order.aggregate.vo.OrderItem;
import com.example.demo.application.domain.order.command.CreateOrderCommand;
import com.example.demo.application.shared.dto.OrderQueriedView;
import com.example.demo.config.config.MapStructConfiguration;
import com.example.demo.iface.dto.req.CreateOrderItemResource;
import com.example.demo.iface.dto.req.CreateOrderResource;
import com.example.demo.infra.projection.order.OrderItemView;
import com.example.demo.infra.projection.order.OrderView;

/**
 * OrderMapper - 訂單模組物件映射器
 * <p>
 * 整合了查詢端 (Query Side) 的視圖轉換與指令端 (Command Side) 的防腐層轉換。
 * </p>
 */
@Mapper(componentModel = "spring", config = MapStructConfiguration.class)
public interface OrderMapper {

	// ##### 查詢端 (Query Side) #####

	/**
	 * 將訂單投影實體轉為明細 DTO
	 * <p>
	 * MapStruct 會自動調用 {@link #toItemDto} 來處理內部的品項清單。
	 * </p>
	 * 
	 * @param entity 訂單投影實體 (包含關聯品項)
	 * @return 完整的訂單查詢視圖
	 */
	OrderQueriedView toQueriedView(OrderView entity);

	/**
	 * 將訂單品項實體轉為品項 DTO (Record)
	 */
	OrderQueriedView.OrderItemQueriedView toItemDto(OrderItemView itemView);

	/**
	 * 批量轉換訂單實體，用於分頁或列表查詢
	 */
	List<OrderQueriedView> transformProjection(List<OrderView> projections);

	// ##### 指令端 (Command Side - ACL) #####

	/**
	 * ACL 轉換：將 API 請求資源轉換為「建立訂單」指令
	 * 
	 * @param resource 前端傳入的下單請求
	 * @return 準備發送至 Command Gateway 的指令
	 */
	@Mapping(target = "orderId", ignore = true) // ID 通常由應用層或聚合根生成
	CreateOrderCommand transformACL(CreateOrderResource resource);

	/**
	 * 將 API 的品項資源轉換為領域模型內部的品項對象
	 */
	OrderItem transform(CreateOrderItemResource resource);

}