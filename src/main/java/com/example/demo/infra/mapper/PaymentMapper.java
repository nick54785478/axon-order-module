package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.example.demo.application.shared.dto.PaymentQueriedView;
import com.example.demo.config.config.MapStructConfiguration;
import com.example.demo.infra.projection.payment.PaymentView;

/**
 * PaymentMapper - 支付模組物件映射器
 * <p>
 * 負責處理支付相關實體 (Projection) 與資料傳輸物件 (DTO) 之間的轉換。
 * </p>
 */
@Mapper(componentModel = "spring", config = MapStructConfiguration.class)
public interface PaymentMapper {

	/**
	 * 將支付投影實體轉換為查詢專用 DTO
	 * 
	 * @param projection 來自資料庫的支付快照
	 * @return 包含支付狀態與金額的 DTO
	 */
	PaymentQueriedView toQueriedView(PaymentView projection);

	/**
	 * 批量轉換支付投影實體
	 * 
	 * @param projections 支付投影實體清單
	 * @return 支付 DTO 清單
	 */
	List<PaymentQueriedView> toQueriedView(List<PaymentView> projections);

}