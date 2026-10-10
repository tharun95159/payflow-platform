
package com.payflow.payment.dto;

import java.math.BigDecimal;

public record AccountResponse(
        Long accountId,
        Long customerId,
        String accountType,
        BigDecimal balance,
        String currency,
        String status
) {
}
