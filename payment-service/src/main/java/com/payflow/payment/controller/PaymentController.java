package com.payflow.payment.controller;

import com.payflow.payment.dto.PaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.payflow.payment.entity.PaymentStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService){
        this.paymentService=paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request){
        PaymentResponse response=paymentService.createPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentReference}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable String paymentReference) {

        PaymentResponse response =
                paymentService.getPaymentByReference(paymentReference);

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{paymentReference}/status")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable String paymentReference,
            @RequestParam PaymentStatus status) {

        PaymentResponse response = paymentService.updatePaymentStatus(
                paymentReference,
                status
        );

        return ResponseEntity.ok(response);
    }

}
