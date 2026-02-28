package com.example.demo.iface.dto.req;

import java.math.BigDecimal;

public record CreateOrderItemResource(String productId, Integer quantity, BigDecimal price) {

}
