package com.example.demo.application.domain.product.query;

/**
 * FindProductsPagedQuery - 分頁查詢產品指令
 * 
 * @param name 產品名稱關鍵字 (模糊查詢)
 * @param page 頁碼 (0 開始)
 * @param size 每頁筆數
 */
public record FindProductsPagedQuery(String name, int page, int size) {
}