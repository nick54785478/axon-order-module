package com.example.demo.iface.dto.res;

import java.util.List;

import com.example.demo.application.shared.dto.ProductQueriedView;

public record ProductListQueriedResource(String code, String message, List<ProductQueriedView> data) {

}
