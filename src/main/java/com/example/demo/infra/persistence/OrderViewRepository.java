package com.example.demo.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.infra.projection.order.OrderView;

public interface OrderViewRepository extends JpaRepository<OrderView, String> {
}