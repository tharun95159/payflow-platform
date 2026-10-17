
package com.payflow.account.service;

import com.payflow.account.dto.AccountRequest;
import com.payflow.account.dto.AccountResponse;
import com.payflow.account.entity.Account;
import com.payflow.account.entity.AccountStatus;
import com.payflow.account.entity.AccountType;
import com.payflow.account.entity.TransferRecord;
import com.payflow.account.repository.AccountRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.payflow.account.exception.AccountAlreadyExistsException;
import java.util.Optional;
import com.payflow.account.exception.AccountNotFoundException;
import com.payflow.account.repository.TransferRecordRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRecordRepository transferRecordRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccountSuccessfully() {

        // Step 1: Prepare test input
        AccountRequest request = new AccountRequest();
        request.setAccountId(10001L);
        request.setCustomerId(101L);
        request.setAccountType(AccountType.CHECKING);
        request.setBalance(new BigDecimal("5000.00"));
        request.setCurrency("USD");

        // Step 2: Prepare the fake repository behavior
        when(accountRepository.existsById(10001L))
                .thenReturn(false);

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Step 3: Call the actual service method
        AccountResponse response = accountService.createAccount(request);

        // Step 4: Verify the result
        assertEquals(10001L, response.getAccountId());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertEquals(new BigDecimal("5000.00"), response.getBalance());

        // Step 5: Verify that repository.save() was called
        verify(accountRepository).save(any(Account.class));
    }


    @Test
    void shouldThrowExceptionWhenAccountAlreadyExists() {

        // Step 1: Prepare test input
        AccountRequest request = new AccountRequest();
        request.setAccountId(10001L);
        request.setCustomerId(101L);
        request.setAccountType(AccountType.CHECKING);
        request.setBalance(new BigDecimal("5000.00"));
        request.setCurrency("USD");

        // Step 2: Simulate an existing account
        when(accountRepository.existsById(10001L))
                .thenReturn(true);

        // Step 3: Verify that the exception is thrown
        AccountAlreadyExistsException exception =
                assertThrows(
                        AccountAlreadyExistsException.class,
                        () -> accountService.createAccount(request)
                );

        // Step 4: Verify the error message
        assertTrue(exception.getMessage().contains("10001"));

        // Step 5: Verify that save() was never called
        verify(accountRepository, never())
                .save(any(Account.class));
    }


    @Test
    void shouldGetAccountByIdSuccessfully() {

        // Step 1: Create a sample account
        Account account = Account.builder()
                .accountId(10001L)
                .customerId(101L)
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        // Step 2: Mock the repository response
        when(accountRepository.findById(10001L))
                .thenReturn(Optional.of(account));

        // Step 3: Call the actual service method
        AccountResponse response =
                accountService.getAccountById(10001L);

        // Step 4: Verify the returned account
        assertEquals(10001L, response.getAccountId());
        assertEquals(101L, response.getCustomerId());
        assertEquals(AccountType.CHECKING, response.getAccountType());
        assertEquals(new BigDecimal("5000.00"), response.getBalance());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());

        // Step 5: Verify repository interaction
        verify(accountRepository).findById(10001L);
    }


    @Test
    void shouldThrowExceptionWhenAccountNotFound() {

        // Step 1: Simulate an account that doesn't exist
        when(accountRepository.findById(99999L))
                .thenReturn(Optional.empty());

        // Step 2: Call the service and expect an exception
        AccountNotFoundException exception = assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getAccountById(99999L)
        );

        // Step 3: Verify the exception message
        assertTrue(exception.getMessage().contains("99999"));

        // Step 4: Verify repository interaction
        verify(accountRepository).findById(99999L);
    }


    @Test
    void shouldTransferFundsSuccessfully() {

        // Arrange: Create two test accounts
        Account source = new Account();
        source.setAccountId(10001L);
        source.setBalance(new BigDecimal("5000.00"));
        source.setCurrency("USD");
        source.setStatus(AccountStatus.ACTIVE);

        Account destination = new Account();
        destination.setAccountId(20001L);
        destination.setBalance(new BigDecimal("2000.00"));
        destination.setCurrency("USD");
        destination.setStatus(AccountStatus.ACTIVE);

        // Mock the database operations
        when(transferRecordRepository.existsById("PAY-TEST-001"))
                .thenReturn(false);

        when(accountRepository.findByIdForUpdate(10001L))
                .thenReturn(Optional.of(source));

        when(accountRepository.findByIdForUpdate(20001L))
                .thenReturn(Optional.of(destination));

        // Act: Transfer $500
        accountService.transferFunds(
                "PAY-TEST-001",
                10001L,
                20001L,
                new BigDecimal("500.00")
        );

        // Assert: Verify the new balances
        assertEquals(
                0,
                new BigDecimal("4500.00")
                        .compareTo(source.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("2500.00")
                        .compareTo(destination.getBalance())
        );

        // Verify both accounts were saved
        verify(accountRepository).save(source);
        verify(accountRepository).save(destination);

        // Verify the transfer record was saved
        verify(transferRecordRepository)
                .saveAndFlush(any(TransferRecord.class));
    }

    @Test
    void shouldRejectTransferWhenBalanceIsInsufficient() {

        // Arrange: Create two accounts
        Account source = new Account();
        source.setAccountId(10001L);
        source.setBalance(new BigDecimal("5000.00"));
        source.setCurrency("USD");
        source.setStatus(AccountStatus.ACTIVE);

        Account destination = new Account();
        destination.setAccountId(20001L);
        destination.setBalance(new BigDecimal("2000.00"));
        destination.setCurrency("USD");
        destination.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findByIdForUpdate(10001L))
                .thenReturn(Optional.of(source));

        when(accountRepository.findByIdForUpdate(20001L))
                .thenReturn(Optional.of(destination));

        // Act: Attempt to transfer $6000
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.transferFunds(
                        "PAY-TEST-002",
                        10001L,
                        20001L,
                        new BigDecimal("6000.00")
                )
        );

        // Assert: Verify the error message
        assertEquals(
                "Insufficient balance in source account",
                exception.getMessage()
        );

        // Verify balances remain unchanged
        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(source.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(destination.getBalance())
        );

        // Verify nothing was saved
        verify(accountRepository, never()).save(any(Account.class));

        verify(transferRecordRepository, never())
                .saveAndFlush(any(TransferRecord.class));
    }


    @Test
    void shouldRejectDuplicateTransferReference() {

        // Arrange: Simulate an existing transfer
        String transferReference = "PAY-TEST-003";

        when(transferRecordRepository.existsById(transferReference))
                .thenReturn(true);

        // Act: Attempt the same transfer again
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.transferFunds(
                        transferReference,
                        10001L,
                        20001L,
                        new BigDecimal("500.00")
                )
        );

        // Assert: Verify duplicate error message
        assertEquals(
                "Transfer already exists with reference: " + transferReference,
                exception.getMessage()
        );

        // Verify no accounts were locked or modified
        verify(accountRepository, never())
                .findByIdForUpdate(anyLong());

        verify(accountRepository, never())
                .save(any(Account.class));

        // Verify no new transfer record was saved
        verify(transferRecordRepository, never())
                .saveAndFlush(any(TransferRecord.class));
    }


}
