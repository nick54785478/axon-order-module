package com.example.demo.iface.dto.req;

import jakarta.validation.constraints.NotNull;

/**
 * ConfirmShipmentResource - 接收前端的版本號
 */
public record ConfirmShipmentResource(@NotNull String orderId, @NotNull(message = "版本號不可為空") Long version) {
}