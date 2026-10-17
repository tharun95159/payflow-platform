
package com.payflow.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_records")
public class TransferRecord {

    @Id
    @Column(name = "transfer_reference", nullable = false)
    private String transferReference;

    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;


    public TransferRecord() {
    }

    public TransferRecord(
            String transferReference,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            LocalDateTime createdAt) {

        this.transferReference = transferReference;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

}
