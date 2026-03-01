package com.example.demo.iface.dto.req;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record CreateOrderResource(List<CreateOrderItemResource> items, @NotNull(message = "金額不可為空") BigDecimal amount) {
}
