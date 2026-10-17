
package com.payflow.account.integration;

import com.payflow.account.entity.Account;
import com.payflow.account.entity.AccountStatus;
import com.payflow.account.entity.AccountType;
import com.payflow.account.repository.AccountRepository;
import com.payflow.account.repository.TransferRecordRepository;
import com.payflow.account.service.AccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
class AccountTransferIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRecordRepository transferRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldTransferFundsSuccessfully() {

        // Step 1: Create two test accounts
        LocalDateTime now = LocalDateTime.now();

        Account source = Account.builder()
                .accountId(10001L)
                .customerId(101L)
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Account destination = Account.builder()
                .accountId(20001L)
                .customerId(102L)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("2000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(source);
        accountRepository.save(destination);

        // Step 2: Transfer $500
        accountService.transferFunds(
                "TEST-TRANSFER-001",
                10001L,
                20001L,
                new BigDecimal("500.00")
        );

        // Step 3: Read updated balances from PostgreSQL
        Account updatedSource =
                accountRepository.findById(10001L).orElseThrow();

        Account updatedDestination =
                accountRepository.findById(20001L).orElseThrow();

        // Step 4: Verify balances
        assertEquals(
                0,
                new BigDecimal("4500.00")
                        .compareTo(updatedSource.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("2500.00")
                        .compareTo(updatedDestination.getBalance())
        );

        // Step 5: Verify transfer record
        assertTrue(
                transferRecordRepository.existsById("TEST-TRANSFER-001")
        );
    }

    @Test
    void shouldNotTransferWhenBalanceIsInsufficient() {

        LocalDateTime now = LocalDateTime.now();

        Account source = Account.builder()
                .accountId(10001L)
                .customerId(101L)
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Account destination = Account.builder()
                .accountId(20001L)
                .customerId(102L)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("2000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(source);
        accountRepository.save(destination);

        assertThrows(IllegalArgumentException.class, () ->
                accountService.transferFunds(
                        "TEST-TRANSFER-FAILED-001",
                        10001L,
                        20001L,
                        new BigDecimal("6000.00")
                )
        );

        Account updatedSource =
                accountRepository.findById(10001L).orElseThrow();

        Account updatedDestination =
                accountRepository.findById(20001L).orElseThrow();

        assertEquals(0, new BigDecimal("5000.00")
                .compareTo(updatedSource.getBalance()));

        assertEquals(0, new BigDecimal("2000.00")
                .compareTo(updatedDestination.getBalance()));

        assertFalse(transferRecordRepository
                .existsById("TEST-TRANSFER-FAILED-001"));
    }

    @Test
    void shouldRollbackTransferWhenDatabaseFails() {

        LocalDateTime now = LocalDateTime.now();

        Account source = Account.builder()
                .accountId(30001L)
                .customerId(301L)
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(source);

        Account destination = Account.builder()
                .accountId(40001L)
                .customerId(401L)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("2000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(destination);

        jdbcTemplate.execute(
                "ALTER TABLE transfer_records " +
                        "DROP CONSTRAINT IF EXISTS reject_rollback_test_transfer"
        );

        jdbcTemplate.execute(
                "ALTER TABLE transfer_records " +
                        "ADD CONSTRAINT reject_rollback_test_transfer " +
                        "CHECK (transfer_reference <> 'TEST-ROLLBACK-001')"
        );

        try {

        assertThrows(Exception.class, () ->
                accountService.transferFunds(
                        "TEST-ROLLBACK-001",
                        30001L,
                        40001L,
                        new BigDecimal("500.00")
                )
        );

        entityManager.clear();

        Account updatedSource =
                accountRepository.findById(30001L).orElseThrow();

        Account updatedDestination =
                accountRepository.findById(40001L).orElseThrow();

        assertEquals(0, new BigDecimal("5000.00")
                .compareTo(updatedSource.getBalance()));

        assertEquals(0, new BigDecimal("2000.00")
                .compareTo(updatedDestination.getBalance()));

        assertFalse(
                transferRecordRepository.existsById("TEST-ROLLBACK-001")
        );

        } finally {

        jdbcTemplate.execute(
                "ALTER TABLE transfer_records " +
                        "DROP CONSTRAINT IF EXISTS reject_rollback_test_transfer"
        );

        }


    }
}
