package com.example.demo.application.shared.dto;

import java.util.List;

/**
 * PageQueriedView - 通用分頁結果封裝
 */
public record PageQueriedView<T>(List<T> content, long totalElements, int totalPages, int currentPage) {
}
