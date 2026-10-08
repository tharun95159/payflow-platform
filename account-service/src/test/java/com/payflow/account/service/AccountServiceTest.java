
package com.payflow.account.service;

import com.payflow.account.dto.AccountRequest;
import com.payflow.account.dto.AccountResponse;
import com.payflow.account.entity.Account;
import com.payflow.account.entity.AccountStatus;
import com.payflow.account.entity.AccountType;
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

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

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



}
