package com.example.demo.infra.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.infra.projection.payment.PaymentView;

public interface PaymentViewRepository extends JpaRepository<PaymentView, String> {

	List<PaymentView> findByOrderId(String orderId);

}
