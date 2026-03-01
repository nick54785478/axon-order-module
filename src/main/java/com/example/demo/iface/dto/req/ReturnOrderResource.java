package com.example.demo.iface.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * ReturnOrderResource - 退貨發起來源
 */
public record ReturnOrderResource(@NotNull(message = "orderId 不可為空") String orderId,
		@NotNull(message = "版本號不可為空") Long version, // 樂觀鎖防護
		@NotBlank(message = "原因不可為空") String reason) {
}
