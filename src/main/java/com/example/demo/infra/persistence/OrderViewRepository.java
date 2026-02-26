package com.example.demo.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.application.domain.order.projection.OrderView;

public interface OrderViewRepository extends JpaRepository<OrderView, String> {
}