package com.example.demo.application.shared.exception;

/**
 * InsufficientFundsException - 餘額不足異常
 */
public class InsufficientFundsException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InsufficientFundsException(String message) {
		super(message);
	}
}