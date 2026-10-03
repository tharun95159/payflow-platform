package com.payflow.payment.service;

import com.payflow.payment.dto.PaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.entity.PaymentStatus;
import com.payflow.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.payflow.payment.exception.PaymentNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = Mockito.mock(PaymentRepository.class);
        paymentService = new PaymentService(paymentRepository);
    }

    @Test
    void shouldCreatePaymentSuccessfully() {

        PaymentRequest request = new PaymentRequest();
        request.setFromAccountId(10001L);
        request.setToAccountId(20001L);
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("USD");

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response =
                paymentService.createPayment(request);

        assertEquals(10001L, response.getFromAccountId());
        assertEquals(20001L, response.getToAccountId());
        assertEquals(new BigDecimal("500.00"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals(PaymentStatus.INITIATED, response.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFound() {

        String paymentReference = "PAY-NOTFOUND";

        when(paymentRepository.findByPaymentReference(paymentReference))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.getPaymentByReference(paymentReference)
        );
    }
}