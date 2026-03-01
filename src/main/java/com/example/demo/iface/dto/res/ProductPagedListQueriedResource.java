package com.example.demo.iface.dto.res;

import com.example.demo.application.shared.dto.PageQueriedView;
import com.example.demo.application.shared.dto.ProductQueriedView;

public record ProductPagedListQueriedResource(String code, String message, PageQueriedView<ProductQueriedView> data) {

}
