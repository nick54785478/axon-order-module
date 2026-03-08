package com.example.demo.application.shared.dto;

import java.util.List;

/**
 * 專為產品查詢設計的具體分頁視圖，避免泛型序列化問題
 */
public record ProductPageQueriedView(
    List<ProductQueriedView> content,
    long totalElements,
    int totalPages,
    int currentPage
) {}