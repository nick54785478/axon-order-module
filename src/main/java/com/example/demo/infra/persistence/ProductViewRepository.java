package com.example.demo.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.application.domain.product.projection.ProductView;

public interface ProductViewRepository extends JpaRepository<ProductView, String> {

}
