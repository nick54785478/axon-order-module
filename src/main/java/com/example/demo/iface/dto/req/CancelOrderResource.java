package com.example.demo.iface.dto.req;

import jakarta.validation.constraints.NotNull;

/**
 * CancelOrderResource - 訂單取消請求資源
 * 
 * @param version 必填！代表客戶端目前看見的訂單版本號，用於樂觀鎖校驗。
 */
public record CancelOrderResource( @NotNull(message = "orderId 不可為空") String orderId, @NotNull(message = "版本號不可為空") Long version) {
}