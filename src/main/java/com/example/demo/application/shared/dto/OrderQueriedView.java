package com.example.demo.application.shared.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderQueriedView {

	private String orderId;

	private BigDecimal amount;

	private String status;

}
