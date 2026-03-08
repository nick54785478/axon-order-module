package com.example.demo.infra.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.infra.projection.product.ProductView;

public interface ProductViewRepository extends JpaRepository<ProductView, String> {

	// 支援名稱模糊搜尋且分頁
	Page<ProductView> findByNameContainingIgnoreCase(String name, Pageable pageable);

	// 若名稱為空則回傳全部分頁
	Page<ProductView> findAll(Pageable pageable);
}
