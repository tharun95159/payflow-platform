package com.payflow.payment.dto;

import com.payflow.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {

    private String paymentReference;

    private Long fromAccountId;

    private Long toAccountId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;

    private LocalDateTime createdAt;
}