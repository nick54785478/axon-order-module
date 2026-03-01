package com.example.demo.iface.dto.req;

import java.math.BigDecimal;

public record CreateProductResource(String name, BigDecimal price, Integer initialStock) {
}