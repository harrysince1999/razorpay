package com.harikesh.razorpay.razorpay.payment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	private final PaymentRepository paymentRepository;

	public PaymentController(PaymentRepository paymentRepository) {
		this.paymentRepository = paymentRepository;
	}

	@GetMapping
	public List<Payment> getPayments() {
		return paymentRepository.findAll();
	}

	@GetMapping("/{id}")
	public Payment getPayment(@PathVariable Long id) {
		return paymentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Payment createPayment(@RequestBody Payment payment) {
		return paymentRepository.save(payment);
	}
}
