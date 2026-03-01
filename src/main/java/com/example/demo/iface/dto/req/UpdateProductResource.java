package com.example.demo.iface.dto.req;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * UpdateProductResource - 產品更新請求資源
 * 
 * @param version 必填！代表客戶端「看見」的資料版本。
 * @param name    新的產品名稱
 * @param price   新的產品單價
 */
public record UpdateProductResource(@NotNull(message = "版本號不可為空") Long version,
		@NotBlank(message = "產品名稱不可為空") String name, @Positive(message = "價格必須大於零") BigDecimal price) {
}

// 深度解析：版本號為什麼要由外部傳入？
// 答: 在 Event Sourcing 中，版本號（Aggregate Version）不僅僅是一個數字，它是 「樂觀鎖（Optimistic Locking）」 的靈魂。
