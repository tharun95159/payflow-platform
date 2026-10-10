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
import com.payflow.payment.client.AccountClient;
import com.payflow.payment.dto.AccountResponse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private AccountClient accountClient;
    private PaymentService paymentService;


    @BeforeEach
    void setUp() {

        paymentRepository = Mockito.mock(PaymentRepository.class);

        accountClient = Mockito.mock(AccountClient.class);

        paymentService = new PaymentService(
                paymentRepository,
                accountClient
        );
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


        AccountResponse fromAccount = new AccountResponse(
                10001L,
                101L,
                "CHECKING",
                new BigDecimal("5000.00"),
                "USD",
                "ACTIVE"
        );

        AccountResponse toAccount = new AccountResponse(
                20001L,
                102L,
                "SAVINGS",
                new BigDecimal("2000.00"),
                "USD",
                "ACTIVE"
        );

        when(accountClient.getAccountById(10001L))
                .thenReturn(fromAccount);

        when(accountClient.getAccountById(20001L))
                .thenReturn(toAccount);

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


    @Test
    void shouldRejectPaymentWhenSourceAccountIsInactive() {

        // Step 1: Prepare payment request
        PaymentRequest request = new PaymentRequest();
        request.setFromAccountId(10001L);
        request.setToAccountId(20001L);
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("USD");

        // Step 2: Create an INACTIVE source account
        AccountResponse fromAccount = new AccountResponse(
                10001L,
                101L,
                "CHECKING",
                new BigDecimal("5000.00"),
                "USD",
                "INACTIVE"
        );

        // Step 3: Create an ACTIVE destination account
        AccountResponse toAccount = new AccountResponse(
                20001L,
                102L,
                "SAVINGS",
                new BigDecimal("2000.00"),
                "USD",
                "ACTIVE"
        );

        // Step 4: Mock AccountClient responses
        when(accountClient.getAccountById(10001L))
                .thenReturn(fromAccount);

        when(accountClient.getAccountById(20001L))
                .thenReturn(toAccount);

        // Step 5: Verify payment creation fails
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(request)
        );

        // Step 6: Verify error message
        assertTrue(exception.getMessage()
                .contains("Source account is not active"));

        // Step 7: Verify payment was never saved
        verify(paymentRepository, never())
                .save(any(Payment.class));
    }


    @Test
    void shouldRejectPaymentWhenDestinationAccountIsBlocked() {

        // Step 1: Prepare payment request
        PaymentRequest request = new PaymentRequest();
        request.setFromAccountId(10001L);
        request.setToAccountId(20001L);
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("USD");

        // Step 2: Create ACTIVE source account
        AccountResponse fromAccount = new AccountResponse(
                10001L,
                101L,
                "CHECKING",
                new BigDecimal("5000.00"),
                "USD",
                "ACTIVE"
        );

        // Step 3: Create BLOCKED destination account
        AccountResponse toAccount = new AccountResponse(
                20001L,
                102L,
                "SAVINGS",
                new BigDecimal("2000.00"),
                "USD",
                "BLOCKED"
        );

        // Step 4: Mock AccountClient responses
        when(accountClient.getAccountById(10001L))
                .thenReturn(fromAccount);

        when(accountClient.getAccountById(20001L))
                .thenReturn(toAccount);

        // Step 5: Expect payment rejection
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(request)
        );

        // Step 6: Verify error message
        assertTrue(exception.getMessage()
                .contains("Destination account is not active"));

        // Step 7: Verify payment was not saved
        verify(paymentRepository, never())
                .save(any(Payment.class));
    }


    @Test
    void shouldRejectPaymentWhenBalanceIsInsufficient() {

        // Step 1: Create payment request
        PaymentRequest request = new PaymentRequest();
        request.setFromAccountId(10001L);
        request.setToAccountId(20001L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("USD");

        // Step 2: Mock source account with only $500
        AccountResponse fromAccount = new AccountResponse(
                10001L,
                101L,
                "CHECKING",
                new BigDecimal("500.00"),
                "USD",
                "ACTIVE"
        );

        // Step 3: Mock destination account
        AccountResponse toAccount = new AccountResponse(
                20001L,
                102L,
                "SAVINGS",
                new BigDecimal("2000.00"),
                "USD",
                "ACTIVE"
        );

        // Step 4: Mock Account Service responses
        when(accountClient.getAccountById(10001L))
                .thenReturn(fromAccount);

        when(accountClient.getAccountById(20001L))
                .thenReturn(toAccount);

        // Step 5: Verify payment is rejected
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(request)
        );

        // Step 6: Verify the error message
        assertTrue(exception.getMessage()
                .contains("Insufficient balance"));

        // Step 7: Verify payment was not saved
        verify(paymentRepository, never())
                .save(any(Payment.class));
    }



}