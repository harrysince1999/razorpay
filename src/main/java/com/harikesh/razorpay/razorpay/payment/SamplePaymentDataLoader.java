package com.harikesh.razorpay.razorpay.payment;

import java.math.BigDecimal;
import java.util.logging.Logger;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SamplePaymentDataLoader implements CommandLineRunner {

	private final PaymentRepository paymentRepository;

	public SamplePaymentDataLoader(PaymentRepository paymentRepository) {
		this.paymentRepository = paymentRepository;
	}

	@Override
	public void run(String... args) {
		if (paymentRepository.count() == 0) {
			Logger.getLogger(SamplePaymentDataLoader.class.getName()).info("Loading sample payment data...");
			paymentRepository.save(new Payment("Harikesh", new BigDecimal("499.00"), "INR", "SUCCESS"));
		}
	}
}
