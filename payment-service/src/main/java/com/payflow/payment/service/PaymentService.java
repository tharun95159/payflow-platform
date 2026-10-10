package com.payflow.payment.service;

import com.payflow.payment.dto.PaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.entity.PaymentStatus;
import com.payflow.payment.exception.PaymentNotFoundException;
import com.payflow.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.payflow.payment.client.AccountClient;
import com.payflow.payment.dto.AccountResponse;

@Service
public class PaymentService {


    private final PaymentRepository paymentRepository;
    private final AccountClient accountClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            AccountClient accountClient) {

        this.paymentRepository = paymentRepository;
        this.accountClient = accountClient;
    }


    public PaymentResponse createPayment(PaymentRequest request) {

        LocalDateTime now = LocalDateTime.now();

        validateAccounts(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount()
        );

        Payment payment = Payment.builder()
                .paymentReference(generatePaymentReference())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(PaymentStatus.INITIATED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return mapToResponse(savedPayment);
    }

    private String generatePaymentReference() {
        return "PAY-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    public PaymentResponse getPaymentByReference(String paymentReference) {

        Payment payment = paymentRepository
                .findByPaymentReference(paymentReference)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with reference: " + paymentReference
                        )
                );

        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {

        return PaymentResponse.builder()
                .paymentReference(payment.getPaymentReference())
                .fromAccountId(payment.getFromAccountId())
                .toAccountId(payment.getToAccountId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }


    private void validateAccounts(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount) {

        AccountResponse fromAccount =
                accountClient.getAccountById(fromAccountId);

        AccountResponse toAccount =
                accountClient.getAccountById(toAccountId);

        if (!"ACTIVE".equals(fromAccount.status())) {
            throw new IllegalArgumentException(
                    "Source account is not active: " + fromAccountId
            );
        }


        if (fromAccount.balance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance in source account: " + fromAccountId
            );
        }



        if (!"ACTIVE".equals(toAccount.status())) {
            throw new IllegalArgumentException(
                    "Destination account is not active: " + toAccountId
            );
        }
    }

}