package com.example.demo.application.shared.query;

/**
 * GetProductQuery - 取得特定產品詳細資訊
 * 
 * @param productId 欲查詢的產品唯一識別碼
 */
public record GetProductQuery(String productId) {
}